package geny

import java.io.{InputStream, OutputStream}
import java.lang.Character.MAX_SURROGATE
import java.lang.Character.MIN_SURROGATE

object Internal {
  val defaultMaxBufferStartSize: Int = 64 * 1024
  val defaultMinBufferStartSize: Int = 64
  val defaultBufferGrowthRatio: Int = 4

  /**
   * Transfers data from the given `InputStream` through a to a [[sink]]
   * function through a dynamically sized transfer buffer.
   *
   * The buffer is sized based on the `.available` number on the input stream,
   * clamped a [[minBufferSize]] and [[maxBufferSize]] size, and allowed to
   * grow in increments of 2x if the total amount of bytes transferred exceeds
   * the size of the buffer by [[bufferGrowthRatio]].
   *
   * This should allow efficient processing of `InputStream`s of all sizes,
   * from the tiny to the very large, without over- or under- allocating the
   * size of the transfer buffer.
   */
  def transfer0(src: InputStream,
                sink: (Array[Byte], Int) => Unit,
                minBufferSize: Int = defaultMinBufferStartSize,
                maxBufferSize: Int = defaultMaxBufferStartSize,
                bufferGrowthRatio: Int = defaultBufferGrowthRatio) = {
    def clampBufferSize(n: Int) = {
      if (n < minBufferSize) minBufferSize
      else if (n > maxBufferSize) maxBufferSize
      else n
    }

    var buffer = new Array[Byte](clampBufferSize(src.available()))
    var total = 0
    var read = 0
    while (read != -1) {
      read = src.read(buffer)
      if (read != -1) {
        sink(buffer, read)
        total += read
        if (total > buffer.length * bufferGrowthRatio && buffer.length < maxBufferSize) {
          buffer = new Array[Byte](clampBufferSize(buffer.length * 2))
        }
      }
    }
    src.close()
  }

  def transfer(src: InputStream,
               dest: OutputStream,
               minBufferSize: Int = defaultMinBufferStartSize,
               maxBufferSize: Int = defaultMaxBufferStartSize,
               bufferGrowthRatio: Int = defaultBufferGrowthRatio) = transfer0(
    src,
    dest.write(_, 0, _),
    minBufferSize,
    maxBufferSize,
    bufferGrowthRatio
  )

  // VENDORED FROM GUAVA
  def encodedLength(sequence: String) = { // Warning to maintainers: this implementation is highly optimized.
    val utf16Length = sequence.length
    var utf8Length = utf16Length
    var i = 0
    // This loop optimizes for pure ASCII.
    while (i < utf16Length && sequence.charAt(i) < 0x80) i += 1
    // This loop optimizes for chars less than 0x800.

    while (i < utf16Length) {
      val c = sequence.charAt(i)
      if (c < 0x800) utf8Length += ((0x7f - c) >>> 31) // branch free!
      else {
        utf8Length += encodedLengthGeneral(sequence, i)
        i = utf16Length // break is not supported, just set i to terminate the loop

      }

      i += 1
    }
    if (utf8Length < utf16Length) { // Necessary and sufficient condition for overflow because of maximum 3x expansion
      throw new IllegalArgumentException("UTF-8 length does not fit in int: " + (utf8Length + (1L << 32)))
    }
    utf8Length
  }

  private def encodedLengthGeneral(sequence: String, start: Int) = {
    val utf16Length = sequence.length
    var utf8Length = 0
    var i = start
    while (i < utf16Length) {
      val c = sequence.charAt(i)
      if (c < 0x800) utf8Length += (0x7f - c) >>> 31
      else {
        utf8Length += 2
        // jdk7+: if (Character.isSurrogate(c)) {
        if (MIN_SURROGATE <= c && c <= MAX_SURROGATE) { // Check that we have a well-formed surrogate pair.
          if (Character.codePointAt(sequence, i) == c) throw new IllegalArgumentException(unpairedSurrogateMsg(i))
          i += 1
        }
      }

      i += 1
    }
    utf8Length
  }

  private def unpairedSurrogateMsg(i: Int) = "Unpaired surrogate at index " + i
  // END VENDORED FROM GUAVA
}
