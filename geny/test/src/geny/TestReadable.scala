package geny

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import utest._

object TestReadable extends TestSuite{
  val tests = Tests{
    test("Strings"){
      val out = new ByteArrayOutputStream()
      ("Hello": Readable).writeBytesTo(out)
      ("World": Readable).writeBytesTo(out)
      new String(out.toByteArray) ==> "HelloWorld"
    }
    test("ArrayByte"){
      val out = new ByteArrayOutputStream()
      (Array[Byte](1, 2, 3): Readable).writeBytesTo(out)
      (Array[Byte](4, 5, 6): Readable).writeBytesTo(out)
      out.toByteArray ==> Array[Byte](1, 2, 3, 4, 5, 6)
    }
    test("ByteBuffer"){
      def bb(direct: Boolean, bytes: Int*) = {
        val bb = if (direct) {
          java.nio.ByteBuffer.allocateDirect(bytes.length)
        } else {
          java.nio.ByteBuffer.allocate(bytes.length)
        }
        for ((b, i) <- bytes.zipWithIndex) {
          bb.put(i, b.toByte)
        }
        bb
      }
      test("indirect"){
        val out = new ByteArrayOutputStream()
        (bb(false, 1, 2, 3): Readable).writeBytesTo(out)
        (bb(false, 4, 5, 6): Readable).writeBytesTo(out)
        out.toByteArray ==> Array[Byte](1, 2, 3, 4, 5, 6)
      }
      test("direct"){
        val out = new ByteArrayOutputStream()
        (bb(true, 1, 2, 3): Readable).writeBytesTo(out)
        (bb(true, 4, 5, 6): Readable).writeBytesTo(out)
        out.toByteArray ==> Array[Byte](1, 2, 3, 4, 5, 6)
      }
    }
    test("InputStream"){
      val out = new ByteArrayOutputStream()

      (new ByteArrayInputStream(Array[Byte](1, 2, 3)): Readable).writeBytesTo(out)
      (new ByteArrayInputStream("abc".getBytes): Readable).writeBytesTo(out)
      out.toByteArray ==> (Array[Byte](1, 2, 3) ++ "abc".getBytes)
    }
  }
}
