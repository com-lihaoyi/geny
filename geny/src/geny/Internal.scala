package geny

import java.io.{InputStream, OutputStream}

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
}
