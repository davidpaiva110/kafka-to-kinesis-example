package stream

import config.AppConfig
import domain.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.kafka.ConsumerMessage.CommittableMessage
import org.apache.pekko.kafka.scaladsl.{Committer, Consumer}
import org.apache.pekko.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import org.slf4j.LoggerFactory
import service.MessageTransformer
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest

import java.net.URI
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*
import scala.language.postfixOps

class StreamProcessor(config: AppConfig)(using system: ActorSystem[?]):

  // ***************************************************************************
  // Properties
  // ***************************************************************************

  private val MAX_CONCURRENT_MESSAGES = 500

  private val logger = LoggerFactory.getLogger(getClass)
  private given ExecutionContext = system.executionContext

  private val transformer = MessageTransformer()

  private val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(config.kafka.bootstrapServers)
    .withGroupId(config.kafka.consumerGroupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.kafka.autoOffsetReset)

  private val committerSettings = CommitterSettings(system)

  private val kinesisClient: KinesisAsyncClient =
    KinesisAsyncClient.builder()
      .endpointOverride(URI.create(config.kinesis.endpoint))
      .region(Region.of(config.kinesis.region))
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(
            config.kinesis.accessKeyId,
            config.kinesis.secretAccessKey
          )
        )
      )
      .build()


  // ***************************************************************************
  // Functions
  // ***************************************************************************

  /**
   * Starts the stream processing from Kafka to Kinesis.
   * @return A Future indicating completion of the stream processing
   */
  def run(): Future[Unit] =
    Consumer.committableSource(consumerSettings, Subscriptions.topics(config.kafka.topic))
      // Process messages concurrently with a parallelism of MAX_CONCURRENT_MESSAGES
      .groupedWithin(MAX_CONCURRENT_MESSAGES, 5.seconds).mapConcat(identity)
      .mapAsync(4) { msg => processMessageAsync(msg).map { result => (result, msg.committableOffset) } }
      .map {
        case (result, offset) =>
          result match {
            case Right(_) =>
              logger.info(s"Successfully processed and published message with offset ${offset.partitionOffset.offset}")
              Some(offset)
            case Left(ProcessingError.JsonParseError(errorMsg, cause)) =>
              logger.warn(s"Committing offset to skip unparseable message because: $errorMsg")
              Some(offset)
            case Left(error) =>
              logger.error(s"Error processing message: ${error.toLogString}")
              logger.warn("Not committing offset to allow for reprocessing.")
              None
          }
      }
      // Only commit offsets for successfully processed messages
      .collect { case Some(offset) => offset }
      .via(Committer.flow(committerSettings))
      .recover {
        case ex: Exception =>
          logger.error(s"Stream error occurred but continuing: ${ex.getMessage}", ex)
          Done
      }
      .run()
      .map(_ => ())


  /**
   * Processes a single Kafka message asynchronously: parses, transforms, serializes, and publishes to Kinesis.
   * The Kafka offset will only be committed after successful Kinesis publish.
   *
   * @param msg The Kafka committable message
   * @return Future[Either[ProcessingError, Unit]] - Future completes only after Kinesis publish attempt
   */
  private def processMessageAsync(
                                   msg: CommittableMessage[String, String]
                                 ): Future[Either[ProcessingError, Unit]] =
    val transformationResult = for
      input <- transformer.parseInput(msg.record.value())
      output <- transformer.transform(input)
      json <- transformer.serializeOutput(output)
    yield (json, output.id)

    transformationResult match {
      case Right((json, partitionKey)) =>
        publishToKinesisAsync(json, partitionKey)
      case Left(error) =>
        Future.successful(Left(error))
    }

  /**
   * Publishes data to Kinesis stream asynchronously.
   *
   * @param data The data to publish
   * @param partitionKey The partition key for Kinesis
   * @return Future[Either[ProcessingError, Unit]] - completes when Kinesis responds
   */
  private def publishToKinesisAsync(data: String, partitionKey: String): Future[Either[ProcessingError, Unit]] =
    val request = PutRecordRequest.builder()
      .streamName(config.kinesis.streamName)
      .partitionKey(partitionKey)
      .data(SdkBytes.fromUtf8String(data))
      .build()

    kinesisClient.putRecord(request).asScala
      .map { response =>
        logger.debug(s"Published to Kinesis with sequence number: ${response.sequenceNumber()}")
        Right(())
      }
      .recover {
        case ex: Exception =>
          logger.error(s"Failed to publish to Kinesis: ${ex.getMessage}", ex)
          Left(ProcessingError.KinesisPublishError("Failed to publish to Kinesis", ex))
      }

  /**
   * Gracefully shuts down the stream processor and Kinesis client.
   */
  def shutdown(): Unit =
    kinesisClient.close()
    logger.info("StreamProcessor shutdown complete")