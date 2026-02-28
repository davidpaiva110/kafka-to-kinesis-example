import config.AppConfig
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import stream.StreamProcessor

import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

object Main:
  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit =
    logger.info("Starting Kafka to Kinesis POC...")

    // Load configuration
    AppConfig.load() match
      case Right(config) =>
        logger.info(s"Configuration loaded successfully")
        logger.info(s"Kafka topic: ${config.kafka.topic}")
        logger.info(s"Kinesis stream: ${config.kinesis.streamName}")

        given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "kafka-kinesis-system")
        given ec: ExecutionContext = system.executionContext

        val processor = StreamProcessor(config)

        // Add shutdown hook
        sys.addShutdownHook {
          logger.info("Shutting down...")
          processor.shutdown()
          system.terminate()
          Await.result(system.whenTerminated, 30.seconds)
          logger.info("Shutdown complete")
        }

        // Start processing
        processor.run().onComplete {
          case Success(_) =>
            logger.warn("Stream completed - this should not happen in normal operation !!! Kill David Paiva if this shows on your terminal!")
          case Failure(exception) =>
            logger.error("Stream failed with exception - this should not be executed !!! Kill David Paiva if this shows on your terminal!", exception)
        }

      case Left(error) =>
        logger.error("Failed to load configuration.", error)
        System.exit(1)