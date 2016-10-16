package geny

import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Provides the `geny.Gen` data type, A Generator of elements of type [[A]].
  *
  * [[Gen]] is basically the inverse of
  * a `scala.Iterator`: instead of the core functionality being the pull-based
  * `hasNext` and `next: T` methods, the core is based around the push-based
  * `generate` method. `generate` is basically an extra-customizable version of
  * `foreach`, which allows the person calling it to provide basic control-flow
  * instructions to the upstream Gens.
  *
  * Unlike a `scala.Iterator`, subclasses of [[Gen]] can guarantee any clean
  * up logic is performed by placing it after the `generate` call is made.
  *
  * Transformations on a [[Gen]] are lazy: calling methods like `filter`
  * or `map` do not evaluate the entire Gen, but instead construct a new
  * Gen that delegates to the original. The only methods that evaluate
  * the [[Gen]] are the "Action" methods like
  * `generate`/`foreach`/`find`, or the "Conversion" methods like `toArray` or
  * similar.
  *
  * `generate` takes a function returning `Gen.Action` rather that
  * `Unit`. This allows a downstream Gen to provide basic control
  * commands to the upstream Gens: i.e. [[Gen.End]] to cease
  * enumeration of the upstream Gen. This allows it to avoid traversing and
  * processing elements that the downstream Gen doesn't want/need to see
  * anyway.
  */
trait Gen[+A]{
  /**
    * The core abstract method that defines the [[Gen]] trait. It is
    * essentially the same as `.foreach`, but with additional configurability.
    *
    * @param handleItem      How to handle a single item: performs any desired side
    *                        effects, and returns a [[Gen.Action]] that
    *                        determines how to continue the enumeration.
    * @return an integer stating how many skipped elements from the
    *         `startingSkipped` input remain to be skipped after this
    *         `generate` call has completed.
    */
  def generate(handleItem: A => Gen.Action): Gen.Action

  // Actions
  def foreach(f: A => Unit): Unit = generate{ x =>
    f(x)
    Gen.Continue
  }

  def find(f: A => Boolean): Option[A] = {
    var result: Option[A] = None
    generate{ t =>
      if (!f(t)) Gen.Continue
      else{
        result = Some(t)
        Gen.End
      }
    }
    result
  }
  def exists(f: A => Boolean) = find(f(_)).isDefined
  def contains(a: Any) = exists(_ == a)
  def forall(f: A => Boolean) = !exists(!f(_))
  def count(f: A => Boolean) = {
    var result = 0
    generate{ t =>
      if (f(t)) result +=  1
      Gen.Continue
    }
    result
  }
  def foldLeft[B](start: B)(f: (B, A) => B): B = {
    var result = start
    generate{ t =>
      result = f(result, t)
      Gen.Continue
    }
    result
  }

  def reduceLeft[B >: A](f: (B, A) => B): B = {
    var result: Option[B] = None
    generate{ t =>
      result = result match{
        case None => Some(t)
        case Some(old) => Some(f(old, t))
      }
      Gen.Continue
    }
    result.getOrElse(
      throw new UnsupportedOperationException("empty.reduceLeft")
    )
  }

  // Builders
  def filter(pred: A => Boolean): Gen[A] = new Gen.Filtered(this, pred)
  def map[B](func: A => B): Gen[B] = new Gen.Mapped[B, A](this, func)
  def flatMap[B](func: A => Gen[B]): Gen[B] = new Gen.FlatMapped[B, A](this, func)
  def slice(start: Int, end: Int): Gen[A] = new Gen.Sliced(this, start, end)
  def take(n: Int) = slice(0, n)
  def drop(n: Int) = slice(n, Int.MaxValue)
  def takeWhile(pred: A => Boolean): Gen[A] = new Gen.TakeWhile(this, pred)
  def dropWhile(pred: A => Boolean): Gen[A] = new Gen.DropWhile(this, pred)
  def zipWithIndex: Gen[(A, Int)] = new Gen.ZipWithIndexed(this)
  def zip[B](other: Iterable[B]): Gen[(A, B)] = new Gen.Zipped(this, other)
  def ++[B >: A](other: Gen[B]): Gen[B] = new Gen.Concat(this, other)

  // Conversions
  def head = take(1).toSeq.head
  def toBuffer[B >: A]: mutable.Buffer[B] = {
    val arr = mutable.Buffer.empty[B]
    foreach{arr.append(_)}
    arr

  }
  def toArray[B >: A : ClassTag]: Array[B] = toBuffer.toArray
  def toSeq: Seq[A] = toBuffer
  def toList = toBuffer.toList
  def toSet[B >: A] = toBuffer[B].toSet
  def toVector = toBuffer.toVector
  def mkString(start: String, sep: String, end: String): String = {
    val sb = new StringBuilder
    sb.append(start)
    var first = true
    foreach { x =>
      if (!first) {
        sb.append(sep)
      }
      sb.append(x)
      first = false
    }
    sb.append(end)
    sb.toString()
  }
  def mkString(sep: String): String = mkString("", sep, "")
  def mkString: String = mkString("")
}

object Gen{
  sealed trait Action
  object End extends Action
  object Continue extends Action


  def apply[T](xs: T*) = fromIterable(xs)

  implicit def fromIterable[M[_], T](t: M[T])(implicit convert: (M[T] => Iterable[T])): Gen[T] = new Gen[T]{
    def generate(f: T => Gen.Action) = {
      var last: Gen.Action = Gen.Continue
      val iterator = convert(t).iterator

      while (last == Gen.Continue && iterator.hasNext) {
        last = f(iterator.next())
      }
      last
    }
    override def toString = s"Gen($t)"
  }
  def selfClosing[T](makeIterator: => (Iterator[T], () => Unit)): Gen[T] = new SelfClosing(makeIterator)

  private class SelfClosing[+T](makeIterator: => (Iterator[T], () => Unit)) extends Gen[T]{
    def generate(f: T => Gen.Action) = {
      var last: Gen.Action = Gen.Continue
      val (iterator, onComplete) = makeIterator
      try {
        while (last == Gen.Continue && iterator.hasNext) {
          last = f(iterator.next())
        }
        last
      } finally{
        onComplete()
      }
    }
    override def toString = s"Gen.SelfClosing(...)"
  }



  private class Concat[+T](inner: Gen[T], other: Gen[T]) extends Gen[T] {
    def generate(f: T => Gen.Action) = {
      val res1 = inner.generate(f)
      if (res1 == Gen.End) Gen.End
      else other.generate(f)

    }
    override def toString = s"$inner.zipWithIndex"
  }

  private class ZipWithIndexed[+T](inner: Gen[T]) extends Gen[(T, Int)] {
    def generate(f: ((T, Int)) => Gen.Action) = {
      var i = 0
      inner.generate{t =>
        val res = f(t, i)
        i += 1
        res
      }
    }
    override def toString = s"$inner.zipWithIndex"
  }

  private class Zipped[+T, V](inner: Gen[T], other: Iterable[V]) extends Gen[(T, V)] {
    def generate(f: ((T, V)) => Gen.Action) = {
      val iter = other.iterator
      inner.generate{t =>
        if (!iter.hasNext) Gen.End
        else f(t, iter.next())
      }
    }
    override def toString = s"$inner.zip($other)"
  }

  private class Filtered[+T](inner: Gen[T], pred: T => Boolean) extends Gen[T]{
    def generate(f: T => Gen.Action) = {
      inner.generate{t => if (pred(t)) f(t) else Gen.Continue}
    }
    override def toString = s"$inner.filter($pred)"
  }

  private class Mapped[+T, V](inner: Gen[V], func: V => T) extends Gen[T]{
    def generate(f: T => Gen.Action) = {
      inner.generate{t => f(func(t))}
    }
    override def toString = s"$inner.map($func)"
  }

  private class FlatMapped[+T, V](inner: Gen[V], func: V => Gen[T]) extends Gen[T]{
    def generate(f: T => Gen.Action) = {

      inner.generate{ outerT =>
        func(outerT).generate{ innerT =>
          f(innerT)
        }
      }
    }
    override def toString = s"$inner.map($func)"
  }

  private class Sliced[+T](inner: Gen[T], start: Int, end: Int) extends Gen[T]{
    def generate(f: T => Gen.Action) = {
      var count = 0

      inner.generate{t =>
        if (count < start){
          count += 1
          Gen.Continue
        }else if (count < end){
          count += 1
          if (count != end) f(t)
          else {
            // If we've reached the limit of our slice, evaluate `f` once but
            // then `End` immediately. This saves us having to consume one more
            // item from `inner`, only to ignore/discard it when we realize
            // we're done.
            f(t)
            Gen.End
          }
        } else Gen.End
      }
    }
    override def toString = s"$inner.slice($start, $end)"
  }

  private class TakeWhile[+T](inner: Gen[T], pred: T => Boolean) extends Gen[T]{
    def generate(f: T => Gen.Action) = {
      inner.generate{t =>
        if (pred(t)) {
          f(t)
        } else {
          Gen.End
        }
      }
    }
    override def toString = s"$inner.takeWhile($pred)"
  }

  private class DropWhile[+T](inner: Gen[T], pred: T => Boolean) extends Gen[T]{
    def generate(f: T => Gen.Action) = {
      var started = false
      inner.generate{t =>
        if (!started) {
          if (pred(t)) Gen.Continue
          else {
            started = true
            f(t)
          }
        }else f(t)
      }
    }
    override def toString = s"$inner.dropWhile($pred)"
  }

}
