Geny 0.1.1
==========

```scala
"com.lihaoyi" %% "geny" % "0.1.1"
"com.lihaoyi" %%% "geny" % "0.1.1" // Scala.js
```
Provides the `geny.Generator[A]` data type, a Generator of elements of type `A`.

`Generator` is basically the inverse of
a `scala.Iterator`: instead of the core functionality being the pull-based
`hasNext` and `next: T` methods, the core is based around the push-based
`generate` method, which is similar to `foreach` with some tweaks.

Unlike a `scala.Iterator`, subclasses of `Generator` can guarantee any clean
up logic is performed by placing it after the `generate` call is made. This is
useful for using `Generator`s to model streaming data from files or other
sources that require cleanup: the most common alternative, `scala.Iterator`,
has no way of guaranteeing that the file gets properly closed after reading.
Even so called "self-closing iterators" that close the file after the iterator
is exhausted fail to close the files if the developer uses `.head` or `.take`
to access the first few elements of the iterator, and never exhausts it.

Although `geny.Generator` is not part of the normal collections hierarchy, the
API i intentionally modelled after that of `scala.Iterator` and should be
mostly drop-in, with conversion functions provided where you need to interact
with APIs using the standard Scala collections.

Geny is intentionally a tiny library with one file and zero dependencies,
so you can depend on it (or even copy-paste it into your project) without
fear of taking on unknown heavyweight dependencies.

## Usage

### Construction
The two simplest ways to construct a `Generator` are via the `Generator(...)`
and `Generator.from` constructors:

```scala
import geny.Generator

scala> Generator(0, 1, 2)
res1: geny.Generator[Int] = Generator(WrappedArray(0, 1, 2))

scala> Generator.from(Seq(1, 2, 3)) // pass in any iterable or iterator
res2: geny.Generator[Int] = Generator(List(1, 2, 3))
```

If you need a `Generator` for a source that needs cleanup (closing
file-handles, database connections, etc.) you can use the
`Generator.selfClosing` constructor:

```scala
scala> class DummyCloseableSource{
     |   val iterator = Iterator(1, 2, 3, 4, 5, 6, 7, 8, 9)
     |   var closed = false
     |   def close() = {
     |     closed = true
     |   }
     | }
defined class DummyCloseableSource

scala> val g = Generator.selfClosing{
     |   val closeable = new DummyCloseableSource()
     |   (closeable.iterator, () => closeable.close())
     | }
g: geny.Generator[Int] = Gen.SelfClosing(...)
```

This constructor takes a block that will be called to generate a tuple of an
`Iterator[T]` and a cleanup function of type `() => Unit`. Each time the
`Generator` is evaluated:

- A new pair of `(Iterator[T], () => Unit)` is created using this block
- The iterator is used to generate however many elements are necessary
- the cleanup function is called.


### Terminal Operations

Transformations on a `Generator` are lazy: calling methods like `filter`
or `map` do not evaluate the entire Generator, but instead construct a new
Generator that delegates to the original. The only methods that evaluate
the `Generator` are the "terminal operation" methods like
`foreach`/`find`, or the "Conversion" methods like `toArray` or
similar. In this way, `Generator` behaves similarly to `Iterator`, whose
`map`/`filter` methods are also lazy until terminal oepration is called.

Terminal operations include the following:

```scala
scala> Generator(0, 1, 2).toSeq
res3: Seq[Int] = ArrayBuffer(0, 1, 2)

scala> Generator(0, 1, 2).reduceLeft(_ + _)
res4: Int = 3

scala> Generator(0, 1, 2).foldLeft(0)(_ + _)
res5: Int = 3

scala> Generator(0, 1, 2).exists(_ == 3)
res6: Boolean = false

scala> Generator(0, 1, 2).count(_ > 0)
res7: Int = 2

scala> Generator(0, 1, 2).forall(_ >= 0)
res8: Boolean = true
```

Overall, they behave mostly the same as on the standard Scala collections.
Not every method is supported, but even those that aren't provided can easily
be re-implemented using `foreach` and the other methods available.

### Transformations

Transformations on a `Generator` are lazy: they do not immediately return a
result, and only build up a computation:

```scala
scala> Generator(0, 1, 2).map(_ + 1)
res9: geny.Generator[Int] = Generator(WrappedArray(0, 1, 2)).map(<function1>)

scala> Generator(0, 1, 2).map{x => println(x); x + 1}
res10: geny.Generator[Int] = Generator(WrappedArray(0, 1, 2)).map(<function1>)
```

This computation will be evaluated when one of the
[Terminal Operation](#terminal-operation)s described above is called:

```scala
scala> res10.toSeq
0
1
2
res11: Seq[Int] = ArrayBuffer(1, 2, 3)
```

Most of the common operations on the Scala collections are supported:

```scala
scala> (Generator(0, 1, 2).filter(_ % 2 == 0).map(_ * 2).drop(2) ++
       Generator(5, 6, 7).map(_.toString.toSeq).flatMap(x => x))
res12: geny.Generator[AnyVal] = Generator(WrappedArray(0, 1, 2)).filter(<function1>).map(<function1>).slice(2, 2147483647) ++ Generator(WrappedArray(5, 6, 7)).map(<function1>).map(<function1>)

scala> res12.toSeq
res13: Seq[AnyVal] = ArrayBuffer(5, 6, 7)

scala> Generator(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).flatMap(i => i.toString.toSeq).takeWhile(_ != '6').zipWithIndex.filter(_._1 != '2')
res14: geny.Generator[(Char, Int)] = Generator(WrappedArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)).map(<function1>).takeWhile(<function1>).zipWithIndex.filter(<function1>)

scala> res14.toVector
res15: Vector[(Char, Int)] = Vector((0,0), (1,1), (3,3), (4,4), (5,5))
```

As you can see, you can `flatMap`, `filter`, `map`, `drop`, `takeWhile`, `++`
and call other methods on the `Generator`, and it simply builds up the
computation without running it. Only when a terminal operation like
`toSeq` or `toVector` is called is it finally evaluated into a result.

Note that a `geny.Generator` is immutable, and is thus never exhausted.
However, it also does not perform any memoization or caching, and so calling
a terminal operation like `.toSeq` on a `Generator` multiple times will
evaluate any preceding transformations multiple times. If you do not want this
to be the case, call `.toSeq` to turn it into a concrete sequence and work with
that.

### Self Closing Generators

One major use case of `geny.Generator` is to ensure resources involved in
streaming results from some external source get properly cleaned up. For
example, using `scala.io.Source`, we can get a `scala.Iterator` over the
lines of a file. For example, you may define a helper function like this:

```scala
def getFileLines(path: String): Iterator[String] = {
  val s = scala.io.Source.fromFile(path)(charSet)
  s.getLines()
}
```

However, this is incorrect: you never close the source `s`, and thus if you
call this lots of times, you end up leaving tons of open file handles! If you
are lucky this will crash your program; if you are unlucky it will hang your
kernel and force you to reboot your computer.

One solution to this would be to simply not write helper functions: everyone
who wants to read from a file must instantiate the `scala.io.Source`
themselves, and manually cleanup themselves. This is a possible solution, but
is tedious and annoying. Another possible solution is to have the `Iterator`
close the `io.Source` itself when exhausted, but this still leaves open the
possibility that the caller will use `.head` or `.take` on the iterator: a
perfectly reasonable thing to do if you don't need all the output, but one
that would leave a "self-closing" iterator open and still leaking file handles.

Using `geny.Generator`s, the helper function can instead return a
`Generator.selfClosing`:

```scala
def getFileLines(path: String): geny.Generator[String] = Generator.selfClosing{
  val s = scala.io.Source.fromFile(path)(charSet)
  (s.getLines(), () => s.close())
}
```

The caller can then use normal collection operations on the returned
`geny.Generator`: `map` it, `filter` it, `take`, `toSeq`, etc. and it will
always be properly opened when a terminal operation is called, the required
operations performed, and properly closed when everything is done.

Changelog
=========

0.1.2
-----

- Add `.reduce`, `.fold`, `.sum`, `.product`, `.min`, `.max`, `.minBy`, `.maxBy`
- Rename `.fromIterable` to `.from`, make it also take `Iterator`s

0.1.1
-----

- Publish for Scala 2.12.0

0.1.0
-----

- First release
