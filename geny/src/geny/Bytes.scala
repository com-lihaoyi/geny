package geny

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}

/**
 * Trivial wrapper around `Array[Byte]` with sane equality and useful toString
 */
class Bytes(val array: Array[Byte]){
  override def equals(other: Any) = other match{
    case otherBytes: Bytes => java.util.Arrays.equals(array, otherBytes.array)
    case _ => false
  }

  override def hashCode(): Int = java.util.Arrays.hashCode(array)

  override def toString = try {
    StandardCharsets.UTF_8
      .newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT)
      .decode(ByteBuffer.wrap(array))
      .toString
  } catch (_ =>    s"0x${new BigInteger(1, array).toString(16).toUpperCase}")
}
