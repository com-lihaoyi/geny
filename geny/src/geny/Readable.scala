package geny

import java.nio.charset.StandardCharsets

import scala.io.Codec

/**
 * Encapsulates an `Array[Byte]` and provides convenience methods for
 * reading the data out of it.
 */
trait Readable {

  def bytes: Array[Byte]

  @deprecated("Use .text()")
  def string(): String = text(StandardCharsets.UTF_8)

  @deprecated("Use .text()")
  def string(codec: Codec): String = new String(bytes, codec.charSet)

  def text(): String = text(StandardCharsets.UTF_8)
  def text(codec: Codec): String = new String(bytes, codec.charSet)

  def trim(): String = trim(StandardCharsets.UTF_8)
  def trim(codec: Codec): String = text(codec).trim

  def lines(): Vector[String] = lines(StandardCharsets.UTF_8)
  def lines(codec: Codec): Vector[String] = Predef.augmentString(text(codec)).lines.toVector
}
object Readable{
  case class Chunks(chunks: Seq[Bytes]) extends Readable{
    val bytes = chunks.iterator.map(_.array).toArray.flatten
  }
}