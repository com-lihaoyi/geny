package geny
import java.io.{InputStream, OutputStream, OutputStreamWriter}
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
}

object Writable{
  implicit class StringByteSource(s: String) extends Writable{
    def writeBytesTo(out: OutputStream) = {
      val pw = new OutputStreamWriter(out, StandardCharsets.UTF_8)
      pw.write(s)
      pw.flush()
    }

  }
  implicit class ByteArrayByteSource(a: Array[Byte]) extends Writable{
     def writeBytesTo(out: OutputStream) = out.write(a)
  }
  implicit class InputStreamByteSource(i: InputStream) extends Writable{
     def writeBytesTo(out: OutputStream) = Internal.transfer(i, out)
  }
}