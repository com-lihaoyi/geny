package geny
import java.io.{ByteArrayInputStream, InputStream, OutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import sun.nio.cs.StreamEncoder

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
trait Writable extends Any{
  def writeBytesTo(out: OutputStream): Unit
  def httpContentType: Option[String] = None
  def contentLength: Option[Long] = None
}
object Writable extends LowPriWritable {
  implicit class StringWritable(s: String) extends Writable{
    def writeBytesTo(out: OutputStream): Unit = {

      s.grouped(8192).foreach(ss => out.write(ss.getBytes(StandardCharsets.UTF_8)))
    }
    override def httpContentType = Some("text/plain")
    override def contentLength = Some(Internal.encodedLength(s))
  }

  implicit class ByteArrayWritable(a: Array[Byte]) extends Writable{
    def writeBytesTo(out: OutputStream): Unit = out.write(a)
    override def httpContentType = Some("application/octet-stream")
    override def contentLength = Some(a.length)
  }

  implicit class ByteBufferWritable(buffer: java.nio.ByteBuffer) extends Writable {
    def writeBytesTo(out: OutputStream): Unit = {
      // TODO: there is room for optimization here. We could match on the output
      // stream and in case it has an underlying NIO channel, write the buffer
      // directly to it. E.g.
      //
      // out match {
      //   case fs: java.io.FileOutputStream => fs.getChannel().write(buffer)
      //   case _ =>
      // }
      //
      // This optimization however is not available on ScalaJS, and hence
      // requires a restructuring of this source file.
      val bb = buffer.duplicate().order(buffer.order())
      var tmp = new Array[Byte](8192)
      val length = bb.remaining()
      var count = 0
      while (count < length) {
        val l = math.min(tmp.size, length - count)
        bb.get(tmp, 0, l)
        out.write(tmp, 0, l)
        count += l
      }
    }
    override def httpContentType = Some("application/octet-stream")
    override def contentLength = Some(buffer.remaining())
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

  implicit class ByteBufferReadable(buffer: java.nio.ByteBuffer) extends Readable{
    def readBytesThrough[T](f: InputStream => T): T = {
      val bb = buffer.duplicate().order(buffer.order())

      val is = new InputStream {
        override def read(): Int = if (!bb.hasRemaining()) {
          -1
        } else {
          bb.get() & 0xff
        }
        override def read(bytes: Array[Byte], off: Int, len: Int) = if (!bb.hasRemaining()) {
          -1
        } else {
          val l = math.min(len, bb.remaining())
          bb.get(bytes, off, l)
          l
        }
      }

      f(is)
    }
    override def httpContentType = Some("application/octet-stream")
    override def contentLength = Some(buffer.remaining())
  }

  implicit class InputStreamReadable(i: InputStream) extends Readable{
    def readBytesThrough[T](f: InputStream => T): T = f(i)
  }
}
