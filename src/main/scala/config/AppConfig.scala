package config

import com.typesafe.config.ConfigFactory

import scala.util.Try

case class KafkaConfig(
                        bootstrapServers: String,
                        consumerGroupId: String,
                        topic: String,
                        autoOffsetReset: String
                      )

case class KinesisConfig(
                          endpoint: String,
                          region: String,
                          streamName: String,
                          accessKeyId: String,
                          secretAccessKey: String
                        )

case class AppConfig(
                      kafka: KafkaConfig,
                      kinesis: KinesisConfig
                    )

object AppConfig:
  def load(): Either[Throwable, AppConfig] =
    Try {
      val config = ConfigFactory.load()

      val kafkaConfig = KafkaConfig(
        bootstrapServers = config.getString("kafka.bootstrap-servers"),
        consumerGroupId = config.getString("kafka.consumer.group-id"),
        topic = config.getString("kafka.consumer.topic"),
        autoOffsetReset = config.getString("kafka.consumer.auto-offset-reset")
      )

      val kinesisConfig = KinesisConfig(
        endpoint = config.getString("kinesis.endpoint"),
        region = config.getString("kinesis.region"),
        streamName = config.getString("kinesis.stream-name"),
        accessKeyId = config.getString("kinesis.access-key-id"),
        secretAccessKey = config.getString("kinesis.secret-access-key")
      )

      AppConfig(kafkaConfig, kinesisConfig)
    }.toEither