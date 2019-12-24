package geny

/**
 * Trivial wrapper around `Array[Byte]` with sane equality and useful toString
 */
class Bytes(val array: Array[Byte]){
  override def equals(other: Any) = other match{
    case otherBytes: Bytes => java.util.Arrays.equals(array, otherBytes.array)
    case _ => false
  }
  override def toString = new String(array, java.nio.charset.StandardCharsets.UTF_8)
}