Geny 0.5.0
==========

```scala
"com.lihaoyi" %% "geny" % "0.5.0"
"com.lihaoyi" %%% "geny" % "0.5.0" // Scala.js / native
```

Geny is a small library that provides push-based versions of common standard
library interfaces:

- [geny.Generator[T]](#generator), a push-based version of `scala.Iterator[T]`
- [geny.Writable](#writable), a push-based version of `java.io.InputStream`
  - [geny.Readable](#readable), a pull-based subclass of `Writable`

More background behind the `Writable` and `Readable` interface can be found in
this blog post:

- [Standardizing IO Interfaces for Scala Libraries](http://www.lihaoyi.com/post/StandardizingIOInterfacesforScalaLibraries.html)

## Generator

`Generator` is basically the inverse of a `scala.Iterator`: instead of the core
functionality being the pull-based `hasNext` and `next: T` methods, the core is
based around the push-based `generate` method, which is similar to `foreach`
with some tweaks.

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

## Writable

`geny.Writable` is a minimal interface that can be implemented by any data type
that writes binary output to a `java.io.OutputStream`:

```scala
trait Writable{
  def writeBytesTo(out: OutputStream): Unit
}
```

`Writable` allows for zero-friction zero-overhead streaming data exchange
between these libraries, e.g. allowing you pass Scalatags `Frag`s directly
`os.write`:

```scala
@ import $ivy.`com.lihaoyi::scalatags:0.8.0`, scalatags.Text.all._
import $ivy.$                             , scalatags.Text.all._

@ os.write(os.pwd / "hello.html", html(body(h1("Hello"), p("World!"))))

@ os.read(os.pwd / "hello.html")
res1: String = "<html><body><h1>Hello</h1><p>World!</p></body></html>"
```

Sending `ujson.Value`s directly to `requests.post`

```scala
@ requests.post("https://httpbin.org/post", data = ujson.Obj("hello" -> 1))

@ res2.text
res3: String = """{
  "args": {},
  "data": "{\"hello\":1}",
  "files": {},
  "form": {},
...
```

Serialize Scala data types directly to disk:

```scala
@ os.write(os.pwd / "two.json", upickle.default.stream(Map((1, 2) -> (3, 4), (5, 6) -> (7, 8))))

@ os.read(os.pwd / "two.json")
res5: String = "[[[1,2],[3,4]],[[5,6],[7,8]]]"
```
 
Or streaming file uploads over HTTP:

```scala
@ requests.post("https://httpbin.org/post", data = os.read.stream(os.pwd / "two.json")).text
res6: String = """{
  "args": {},
  "data": "[[[1,2],[3,4]],[[5,6],[7,8]]]",
  "files": {},
  "form": {},
```

All this data exchange happens efficiently in a streaming fashion, without
unnecessarily buffering data in-memory.

`geny.Writable` also allows an implementation to ensure cleanup code runs after
all data has been written (e.g. closing file handles, free-ing managed
resources) and is much easier to implement than `java.io.InputStream`.

Writable has implicit constructors from the following types:

- `String`
- `Array[Byte]`
- `java.io.InputStream`

And implemented by the following libraries:

- [uPickle](https://github.com/lihaoyi/upickle): implemented by `ujson.Value`,
  `upack.Msg`, and can be constructed from JSON-serializable data structures via
  `upickle.default.stream` or `upickle.default.writableBinary`

- [Scalatags](https://github.com/lihaoyi/scalatags): implemented by `scalatags.Text.Tag`

- [Requests-Scala](https://github.com/lihaoyi/requests-scala):
  `requests.get.stream(...)` methods return a [Readable](#readable) subtype of
  `Writable`

- [OS-Lib](https://github.com/lihaoyi/os-lib): `os.read.stream` returns a
  [Readable](#readable) subtype of `Writable`

- [Cask](https://github.com/lihaoyi/cask): `cask.Request` returns a
  [Readable](#readable) subtype of `Writable`

And is accepted by the following libraries:

- [Requests-Scala](https://github.com/lihaoyi/requests-scala) takes `Writable` in the 
  `data =` field of `requests.post` and `requests.put`

- [OS-Lib](https://github.com/lihaoyi/os-lib) accepts a `Writable` in `os.write` and
  the `stdin` parameter of `subprocess.call` or `subprocess.spawn`

- [Cask](https://github.com/lihaoyi/cask): supports returning a `Writable`
  from any Cask endpoint

Any data type that writes bytes out to a `java.io.OutputStream`,
`java.io.Writer`, or `StringBuilder` can be trivially made to implement
`Writable`, which allows it to output data in a streaming fashion without
needing to buffer it in memory. You can also implement `Writable`s in your own
datatypes or accept it in your own method, if you want to inter-operate with
this existing ecosystem of libraries.

### Readable

```scala
trait Readable extends Writable{
  def readBytesThrough[T](f: InputStream => T): T
  def writeBytesTo(out: OutputStream): Unit = readBytesThrough(Internal.transfer(_, out))
}
````

`Readable` is a subtype of [Writable](#writable) that provides an additional
guarantee: not only can it be written to an `java.io.OutputStream`, it can also
be read from by providing a `java.io.InputStream`. Note that the `InputStream`
is scoped and only available within the `readBytesThrough` callback: after that
the `InputStream` will be closed and associated resources (HTTP connections,
file handles, etc.) will be released.

`Readable` is supported by the following built in types:

- `String`
- `Array[Byte]`
- `java.io.InputStream`

Implemented by the following libraries

- [Requests-Scala](https://github.com/lihaoyi/requests-scala):
  `requests.get.stream(...)` methods return a `Readable`

- [OS-Lib](https://github.com/lihaoyi/os-lib): `os.read.stream` returns a
  `Readable`

- [Cask](https://github.com/lihaoyi/cask): `cask.Request` implements `Readable`
  to allow streaming of request data

And is accepted by the following libraries:

- [uPickle](https://github.com/lihaoyi/upickle): `upickle.default.read`,
  `upickle.default.readBinary`, `ujson.read`, and `upack.read` all support
  `Readable`

- [FastParse](https://github.com/lihaoyi/os-lib): `fastparse.parse` accepts
  parsing streaming input from any `Readable`

`Readable` can be used to allow handling of streaming input, e.g. parsing JSON
directly from a file or HTTP request, without needing to buffer the whole file
in memory:

```scala
@ val data = ujson.read(requests.get.stream("https://api.github.com/events"))
data: ujson.Value.Value = Arr(
  ArrayBuffer(
    Obj(
      LinkedHashMap(
        "id" -> Str("11169088214"),
        "type" -> Str("PushEvent"),
        "actor" -> Obj(
...
```

You can also implement `Readable` in your own data types, to allow them to be
seamlessly passed into uPickle or FastParse to be parsed in a streaming fashion.

Note that in exchange for the reduced memory usage, parsing streaming data via
`Readable` in uPickle or FastParse typically comes with a 20-40% CPU performance
penalty over parsing data already in memory, due to the additional book-keeping
necessary with streaming data. Whether it is worthwhile or not depends on your
particular usage pattern.

Changelog
=========

0.5.0
-----

- Improve streaming of `InputStream`s to `OutputStream`s by dynamically sizing
  the transfer buffer.

0.4.2
-----

- Standardize `geny.Readable` as well

0.2.0
-----

- Added [geny.Writable](#writable) interface

0.1.8
-----

- Support for Scala 2.13.0 final

0.1.6
-----

- Add scala-native support

0.1.5
-----

- Add `.withFilter`

0.1.4
-----

- Add `.collect`, `.collectFirst`, `.headOption`  methods

0.1.3
-----

- Allow calling `.count()` without a predicate to count the total number of items
  in the generator

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
