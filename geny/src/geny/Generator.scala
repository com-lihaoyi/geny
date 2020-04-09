package geny

import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Provides the `geny.Gen` data type, A Generator of elements of type [[A]].
  *
  * [[Generator]] is basically the inverse of
  * a `scala.Iterator`: instead of the core functionality being the pull-based
  * `hasNext` and `next: T` methods, the core is based around the push-based
  * `generate` method. `generate` is basically an extra-customizable version of
  * `foreach`, which allows the person calling it to provide basic control-flow
  * instructions to the upstream Gens.
  *
  * Unlike a `scala.Iterator`, subclasses of [[Generator]] can guarantee any clean
  * up logic is performed by placing it after the `generate` call is made.
  *
  * Transformations on a [[Generator]] are lazy: calling methods like `filter`
  * or `map` do not evaluate the entire Gen, but instead construct a new
  * Gen that delegates to the original. The only methods that evaluate
  * the [[Generator]] are the "Action" methods like
  * `generate`/`foreach`/`find`, or the "Conversion" methods like `toArray` or
  * similar.
  *
  * `generate` takes a function returning `Gen.Action` rather that
  * `Unit`. This allows a downstream Gen to provide basic control
  * commands to the upstream Gens: i.e. [[Generator.End]] to cease
  * enumeration of the upstream Gen. This allows it to avoid traversing and
  * processing elements that the downstream Gen doesn't want/need to see
  * anyway.
  */
trait Generator[+A]{
  /**
    * The core abstract method that defines the [[Generator]] trait. It is
    * essentially the same as `.foreach`, but with additional configurability.
    *
    * @param handleItem How to handle a single item: performs any desired side
    *                   effects, and returns a [[Generator.Action]] that
    *                   determines how to continue the enumeration.
    * @return an integer stating how many skipped elements from the
    *         `startingSkipped` input remain to be skipped after this
    *         `generate` call has completed.
    */
  def generate(handleItem: A => Generator.Action): Generator.Action


  // Actions
  def foreach(f: A => Unit): Unit = generate{ x =>
    f(x)
    Generator.Continue
  }

  def find(f: A => Boolean): Option[A] = {
    var result: Option[A] = None
    generate{ t =>
      if (!f(t)) Generator.Continue
      else{
        result = Some(t)
        Generator.End
      }
    }
    result
  }
  def exists(f: A => Boolean) = find(f(_)).isDefined
  def contains(a: Any) = exists(_ == a)
  def forall(f: A => Boolean) = !exists(!f(_))
  def count(f: A => Boolean = ((_: Any) => true)) = {
    var result = 0
    generate{ t =>
      if (f(t)) result +=  1
      Generator.Continue
    }
    result
  }
  def fold[B](start: B)(f: (B, A) => B): B = foldLeft(start)(f)
  def foldLeft[B](start: B)(f: (B, A) => B): B = {
    var result = start
    generate{ t =>
      result = f(result, t)
      Generator.Continue
    }
    result
  }


  def reduce[B >: A](f: (B, A) => B): B = reduceLeft(f)
  def reduceLeft[B >: A](f: (B, A) => B): B = {
    var result: Option[B] = None
    generate{ t =>
      result = result match{
        case None => Some(t)
        case Some(old) => Some(f(old, t))
      }
      Generator.Continue
    }
    result.getOrElse(
      throw new UnsupportedOperationException("empty.reduceLeft")
    )
  }

  // Builders
  def filter(pred: A => Boolean): Generator[A] = new Generator.Filtered(this, pred)
  def withFilter(pred: A => Boolean): Generator[A] = new Generator.Filtered(this, pred)
  def map[B](func: A => B): Generator[B] = new Generator.Mapped[B, A](this, func)
  def flatMap[B](func: A => Generator[B]): Generator[B] = new Generator.FlatMapped[B, A](this, func)
  def collect[B](func: PartialFunction[A, B]): Generator[B] =
    filter(func.isDefinedAt).map(func)
  def collectFirst[B](func: PartialFunction[A, B]): Option[B] =
    filter(func.isDefinedAt).map(func).headOption

  def flatten[V](implicit f: A => Generator[V]) = this.flatMap[V]((x: A) => f(x))
  def slice(start: Int, end: Int): Generator[A] = new Generator.Sliced(this, start, end)
  def take(n: Int) = slice(0, n)
  def drop(n: Int) = slice(n, Int.MaxValue)
  def takeWhile(pred: A => Boolean): Generator[A] = new Generator.TakeWhile(this, pred)
  def dropWhile(pred: A => Boolean): Generator[A] = new Generator.DropWhile(this, pred)
  def zipWithIndex: Generator[(A, Int)] = new Generator.ZipWithIndexed(this)
  def zip[B](other: Iterable[B]): Generator[(A, B)] = new Generator.Zipped(this, other)
  def ++[B >: A](other: Generator[B]): Generator[B] = new Generator.Concat(this, other)

  // Conversions
  def head = take(1).toSeq.head
  def headOption = take(1).toSeq.headOption
  def toBuffer[B >: A]: mutable.Buffer[B] = {
    val arr = mutable.Buffer.empty[B]
    foreach{arr.append(_)}
    arr

  }
  def toArray[B >: A : ClassTag]: Array[B] = toBuffer.toArray
  def toSeq: Seq[A] = toVector
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


  def sum[B >: A](implicit num: Numeric[B]): B = foldLeft(num.zero)(num.plus)

  def product[B >: A](implicit num: Numeric[B]): B = foldLeft(num.one)(num.times)

  def min[B >: A](implicit cmp: Ordering[B]): A = {
    reduceLeft((x, y) => if (cmp.lteq(x, y)) x else y)
  }

  def max[B >: A](implicit cmp: Ordering[B]): A = {
    reduceLeft((x, y) => if (cmp.gteq(x, y)) x else y)
  }

  def maxBy[B](f: A => B)(implicit cmp: Ordering[B]): A = {
    var maxF: B = null.asInstanceOf[B]
    var maxElem: A = null.asInstanceOf[A]
    var first = true

    for (elem <- this) {
      val fx = f(elem)
      if (first || cmp.gt(fx, maxF)) {
        maxElem = elem
        maxF = fx
        first = false
      }
    }
    maxElem
  }
  def minBy[B](f: A => B)(implicit cmp: Ordering[B]): A = {
    var minF: B = null.asInstanceOf[B]
    var minElem: A = null.asInstanceOf[A]
    var first = true

    for (elem <- this) {
      val fx = f(elem)
      if (first || cmp.lt(fx, minF)) {
        minElem = elem
        minF = fx
        first = false
      }
    }
    minElem
  }

}

object Generator{
  sealed trait Action
  object End extends Action
  object Continue extends Action


  def apply[T](xs: T*) = from(xs)

  implicit def from[M[_], T](t: M[T])(implicit convert: (M[T] => TraversableOnce[T])): Generator[T] = new Generator[T]{
    def generate(f: T => Generator.Action) = {
      var last: Generator.Action = Generator.Continue
      val iterator = convert(t).toIterator

      while (last == Generator.Continue && iterator.hasNext) {
        last = f(iterator.next())
      }
      last
    }
    override def toString = s"Generator($t)"
  }


  def selfClosing[T](makeIterator: => (Iterator[T], () => Unit)): Generator[T] = new SelfClosing(makeIterator)

  private class SelfClosing[+T](makeIterator: => (Iterator[T], () => Unit)) extends Generator[T]{
    def generate(f: T => Generator.Action) = {
      var last: Generator.Action = Generator.Continue
      val (iterator, onComplete) = makeIterator
      try {
        while (last == Generator.Continue && iterator.hasNext) {
          last = f(iterator.next())
        }
        last
      } finally{
        onComplete()
      }
    }
    override def toString = s"Gen.SelfClosing(...)"
  }



  private class Concat[+T](inner: Generator[T], other: Generator[T]) extends Generator[T] {
    def generate(f: T => Generator.Action) = {
      val res1 = inner.generate(f)
      if (res1 == Generator.End) Generator.End
      else other.generate(f)

    }
    override def toString = s"$inner ++ $other"
  }

  private class ZipWithIndexed[+T](inner: Generator[T]) extends Generator[(T, Int)] {
    def generate(f: ((T, Int)) => Generator.Action) = {
      var i = 0
      inner.generate{t =>
        val res = f(t, i)
        i += 1
        res
      }
    }
    override def toString = s"$inner.zipWithIndex"
  }

  private class Zipped[+T, V](inner: Generator[T], other: Iterable[V]) extends Generator[(T, V)] {
    def generate(f: ((T, V)) => Generator.Action) = {
      val iter = other.iterator
      inner.generate{t =>
        if (!iter.hasNext) Generator.End
        else f(t, iter.next())
      }
    }
    override def toString = s"$inner.zip($other)"
  }

  private class Filtered[+T](inner: Generator[T], pred: T => Boolean) extends Generator[T]{
    def generate(f: T => Generator.Action) = {
      inner.generate{t => if (pred(t)) f(t) else Generator.Continue}
    }
    override def toString = s"$inner.filter($pred)"
  }

  private class Mapped[+T, V](inner: Generator[V], func: V => T) extends Generator[T]{
    def generate(f: T => Generator.Action) = {
      inner.generate{t => f(func(t))}
    }
    override def toString = s"$inner.map($func)"
  }

  private class FlatMapped[+T, V](inner: Generator[V], func: V => Generator[T]) extends Generator[T]{
    def generate(f: T => Generator.Action) = {

      inner.generate{ outerT =>
        func(outerT).generate{ innerT =>
          f(innerT)
        }
      }
    }
    override def toString = s"$inner.map($func)"
  }

  private class Sliced[+T](inner: Generator[T], start: Int, end: Int) extends Generator[T]{
    def generate(f: T => Generator.Action) = {
      var count = 0

      inner.generate{t =>
        if (count < start){
          count += 1
          Generator.Continue
        }else if (count < end){
          count += 1
          if (count != end) f(t)
          else {
            // If we've reached the limit of our slice, evaluate `f` once but
            // then `End` immediately. This saves us having to consume one more
            // item from `inner`, only to ignore/discard it when we realize
            // we're done.
            f(t)
            Generator.End
          }
        } else Generator.End
      }
    }
    override def toString = s"$inner.slice($start, $end)"
  }

  private class TakeWhile[+T](inner: Generator[T], pred: T => Boolean) extends Generator[T]{
    def generate(f: T => Generator.Action) = {
      inner.generate{t =>
        if (pred(t)) {
          f(t)
        } else {
          Generator.End
        }
      }
    }
    override def toString = s"$inner.takeWhile($pred)"
  }

  private class DropWhile[+T](inner: Generator[T], pred: T => Boolean) extends Generator[T]{
    def generate(f: T => Generator.Action) = {
      var started = false
      inner.generate{t =>
        if (!started) {
          if (pred(t)) Generator.Continue
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
