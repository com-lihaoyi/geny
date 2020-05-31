package geny
import java.io.{ByteArrayInputStream, InputStream, OutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

/**
 * A [[Writable]] is a source of bytes that can be written to an OutputStream.
 *
 * Essentially a push-based version of `java.io.InputStream`, that allows an
 * implementation to guarantee that cleanup logic runs after the bytes are
 * written.
 *
 * [[Writable]] is also much easier to implement than `java.io.InputStream`: any
 * code that previously wrote output to an `ByteArrayOutputStream` or
 * `StringBuilder` can trivially satisfy the [[Writable]] interface. That makes
 * [[Writable]] very convenient to use for allowing zero-friction zero-overhead
 * streaming data exchange between different libraries.
 *
 * [[Writable]] comes with implicit constructors from `Array[Byte]`, `String`
 * and `InputStream`, and is itself a tiny interface with minimal functionality.
 * Libraries using [[Writable]] are expected to extend it to provide additional
 * methods or additional implicit constructors that make sense in their context.
 */
trait Writable{
  def writeBytesTo(out: OutputStream): Unit
  def httpContentType: Option[String] = None
  def contentLength: Option[Long] = None
}
object Writable extends LowPriWritable {
  implicit class StringWritable(s: String) extends Writable{
    def writeBytesTo(out: OutputStream): Unit = {
      s.grouped(1024).foreach(ss => out.write(ss.getBytes(StandardCharsets.UTF_8)))
    }
    override def httpContentType = Some("text/plain")
    override def contentLength = Some(Internal.encodedLength(s))
  }

  implicit class ByteArrayWritable(a: Array[Byte]) extends Writable{
    def writeBytesTo(out: OutputStream): Unit = out.write(a)
    override def httpContentType = Some("application/octet-stream")
    override def contentLength = Some(a.length)
  }
}

trait LowPriWritable{
  implicit def readableWritable[T](t: T)(implicit f: T => Readable): Writable = f(t)
}

/**
 * A [[Readable]] is a source of bytes that can be read from an InputStream
 *
 * A subtype of [[Writable]], every [[Readable]] can be trivially used as a
 * [[Writable]] by transferring the bytes from the InputStream to the OutputStream,
 * but not every [[Writable]] is a [[Readable]].
 *
 * Note that the InputStream is only available inside the `readBytesThrough`, and
 * may be closed and cleaned up (along with any associated resources) once the
 * callback returns.
 */
trait Readable extends Writable{
  def readBytesThrough[T](f: InputStream => T): T
  def writeBytesTo(out: OutputStream): Unit = readBytesThrough(Internal.transfer(_, out))
}
object Readable{
  implicit class StringReadable(s: String) extends Readable{
    def readBytesThrough[T](f: InputStream => T): T = {
      f(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)))
    }
    override def httpContentType = Some("text/plain")
    override def contentLength = Some(Internal.encodedLength(s))
  }

  implicit class ByteArrayReadable(a: Array[Byte]) extends Readable{
    def readBytesThrough[T](f: InputStream => T): T = f(new ByteArrayInputStream(a))
    override def httpContentType = Some("application/octet-stream")
    override def contentLength = Some(a.length)
  }

  implicit class InputStreamReadable(i: InputStream) extends Readable{
    def readBytesThrough[T](f: InputStream => T): T = f(i)
  }
}