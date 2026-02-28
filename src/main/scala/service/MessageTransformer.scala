package service

import domain.*
import io.circe.parser.*
import io.circe.syntax.*

import scala.util.Try

trait MessageTransformer:
  def parseInput(json: String): Either[ProcessingError, InputMessage]
  def transform(input: InputMessage): Either[ProcessingError, OutputMessage]
  def serializeOutput(output: OutputMessage): Either[ProcessingError, String]

object MessageTransformer:
  def apply(): MessageTransformer = new MessageTransformerImpl()

private class MessageTransformerImpl extends MessageTransformer:

  /**
   * Parses JSON string to InputMessage.
   * @param json: The input JSON string
   * @return Either a ProcessingError or the parsed InputMessage on success
   */
  override def parseInput(json: String): Either[ProcessingError, InputMessage] =
    decode[InputMessage](json).left.map { error =>
      ProcessingError.JsonParseError(s"Failed to parse input JSON", error)
    }

  /**
   * Transforms InputMessage to OutputMessage.
   * @param input: The InputMessage to transform
   * @return Either a ProcessingError or the transformed OutputMessage on success
   */
  override def transform(input: InputMessage): Either[ProcessingError, OutputMessage] =
    Try {
      OutputMessage(
        id = input.id,
        name = input.name.toUpperCase,
        value = input.value,
        transformedValue = input.value * 2.0,
        timestamp = input.timestamp,
        processedAt = System.currentTimeMillis()
      )
    }.toEither.left.map { error =>
      ProcessingError.TransformationError("Failed to transform message", error)
    }

  /**
   * Serializes OutputMessage to JSON string.
   * @param output: The OutputMessage to serialize
   * @return Either a ProcessingError or the JSON string on success
   */
  override def serializeOutput(output: OutputMessage): Either[ProcessingError, String] =
    Try(output.asJson.noSpaces).toEither.left.map { error =>
      ProcessingError.JsonParseError("Failed to serialize output JSON", error)
    }