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
    test("InputStream"){
      val out = new ByteArrayOutputStream()

      (new ByteArrayInputStream(Array[Byte](1, 2, 3)): Readable).writeBytesTo(out)
      (new ByteArrayInputStream("abc".getBytes): Readable).writeBytesTo(out)
      out.toByteArray ==> (Array[Byte](1, 2, 3) ++ "abc".getBytes)
    }
  }
}
