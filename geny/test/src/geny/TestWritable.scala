package geny

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import utest._

object TestWritable extends TestSuite{
  val tests = Tests{
    test("Strings"){
      val out = new ByteArrayOutputStream()
      ("Hello": Writable).writeBytesTo(out)
      ("World": Writable).writeBytesTo(out)
      new String(out.toByteArray) ==> "HelloWorld"
    }
    test("ArrayByte"){
      val out = new ByteArrayOutputStream()
      (Array[Byte](1, 2, 3): Writable).writeBytesTo(out)
      (Array[Byte](4, 5, 6): Writable).writeBytesTo(out)
      out.toByteArray ==> Array[Byte](1, 2, 3, 4, 5, 6)
    }
    test("InputStream"){
      val out = new ByteArrayOutputStream()

      (new ByteArrayInputStream(Array[Byte](1, 2, 3)): Writable).writeBytesTo(out)
      (new ByteArrayInputStream("abc".getBytes): Writable).writeBytesTo(out)
      out.toByteArray ==> (Array[Byte](1, 2, 3) ++ "abc".getBytes)
    }
  }
}
