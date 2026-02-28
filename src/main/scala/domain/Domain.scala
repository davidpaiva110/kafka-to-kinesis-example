package domain

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}


enum ProcessingError:
  case JsonParseError(message: String, cause: Throwable)
  case ValidationError(message: String)
  case TransformationError(message: String, cause: Throwable)
  case KinesisPublishError(message: String, cause: Throwable)

object ProcessingError:
  extension (error: ProcessingError)
    def message: String = error match
      case JsonParseError(msg, _) => s"JSON parsing failed: $msg"
      case ValidationError(msg) => s"Validation failed: $msg"
      case TransformationError(msg, _) => s"Transformation failed: $msg"
      case KinesisPublishError(msg, _) => s"Kinesis publish failed: $msg"

    def toLogString: String = error match
      case JsonParseError(msg, cause) => s"${error.message}. Cause: ${cause.getMessage}"
      case ValidationError(msg) => error.message
      case TransformationError(msg, cause) => s"${error.message}. Cause: ${cause.getMessage}"
      case KinesisPublishError(msg, cause) => s"${error.message}. Cause: ${cause.getMessage}"

// Input message from Kafka
case class InputMessage(
                         id: String,
                         name: String,
                         value: Double,
                         timestamp: Long
                       )

object InputMessage:
  given Decoder[InputMessage] = deriveDecoder[InputMessage]
  given Encoder[InputMessage] = deriveEncoder[InputMessage]

// Output message to Kinesis
case class OutputMessage(
                          id: String,
                          name: String,
                          value: Double,
                          transformedValue: Double,
                          timestamp: Long,
                          processedAt: Long
                        )

object OutputMessage:
  given Decoder[OutputMessage] = deriveDecoder[OutputMessage]
  given Encoder[OutputMessage] = deriveEncoder[OutputMessage]