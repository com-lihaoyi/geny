package geny
import utest._
object TestGenerator extends TestSuite{
  val tests = Tests{
    test("toStrings"){
      def check(g: Generator[Int], expected: String) = {
        assert(g.toString == expected)
      }
      // These vary on jvm and js between 2.12 and 2.13
      val x = Generator(0, 1, 2).toString
      assert(
        x == "Generator(WrappedArray(0, 1, 2))" ||
        x == "Generator(WrappedVarArgs(0, 1, 2))" ||
        x == "Generator(ArraySeq(0, 1, 2))"
      )
      check(Generator.from(Range(0, 3).toList), "Generator(List(0, 1, 2))")
    }
    test("unit"){
      def check[T](gen: Generator[T], expected: Seq[T]) = {
        val toSeq = gen.toSeq
        assert(toSeq == expected)
      }
      test("toSeq"){
        check(Generator.from[IndexedSeq, Int](0 until 10), 0 until 10)
      }

      test("find"){
        assert(Generator.from[IndexedSeq, Int](0 until 10).find(_ % 5 == 4) == Some(4))
        assert(Generator.from[IndexedSeq, Int](0 until 10).find(_ % 100 == 40) == None)
      }
      test("exists"){
        assert(Generator.from[IndexedSeq, Int](0 until 10).exists(_ == 4) == true)
        assert(Generator.from[IndexedSeq, Int](0 until 10).exists(_ == 40) == false)
      }
      test("contains"){
        assert(Generator.from[IndexedSeq, Int](0 until 10).contains(4) == true)
        assert(Generator.from[IndexedSeq, Int](0 until 10).contains(40) == false)
      }
      test("forAll"){
        assert(Generator.from[IndexedSeq, Int](0 until 10).forall(_  < 100) == true)
        assert(Generator.from[IndexedSeq, Int](0 until 10).forall(_ < 5) == false)
      }
      test("count"){
        assert(Generator.from[IndexedSeq, Int](0 until 10).count(_  < 100) == 10)
        assert(Generator.from[IndexedSeq, Int](0 until 10).count(_  > 100) == 0)
        assert(Generator.from[IndexedSeq, Int](0 until 10).count(_ < 5) == 5)
        assert(Generator.from[IndexedSeq, Int](0 until 10).count() == 10)
      }

      test("reduceLeft"){
        assert(Generator.from[IndexedSeq, Int](0 until 10).reduceLeft(_ + _) == 45)
        intercept[UnsupportedOperationException](
          Generator.from[IndexedSeq, Int](0 until 0).reduceLeft(_ + _)
        )
      }
      test("foldLeft"){
        assert(Generator.from[IndexedSeq, Int](0 until 10).foldLeft(0)(_ + _) == 45)
        assert(Generator.from[IndexedSeq, Int](0 until 0).foldLeft(0)(_ + _) == 0)
      }


      test("concat"){
        check(
          Generator.from[IndexedSeq, Int](0 until 10) ++ Generator.from[IndexedSeq, Int](0 until 10),
          (0 until 10) ++ (0 until 10)
        )
        check(
          Generator.from[IndexedSeq, Int](0 until 10) ++ Generator.from[IndexedSeq, Int](10 until 20),
          0 until 20
        )
      }
      test("filter"){
        check(Generator.from[IndexedSeq, Int](0 until 10).filter(_ > 5), 6 until 10)
        check(Generator.from[IndexedSeq, Int](0 until 10).withFilter(_ > 5), 6 until 10)
      }
      test("map"){
        check(Generator.from[IndexedSeq, Int](0 until 10).map(_ + 1), 1 until 11)
        check(
          Generator.from[IndexedSeq, Int](0 until 10).map(i => i.toString * i),
          Seq(
            "",
            "1",
            "22",
            "333",
            "4444",
            "55555",
            "666666",
            "7777777",
            "88888888",
            "999999999"
          )
        )
      }
      test("flatMap"){
        val expected = Seq(
          1, 2, 0,
          1, 2, 1,
          1, 2, 2,
          1, 2, 3,
          1, 2, 4,
          1, 2, 5,
          1, 2, 6,
          1, 2, 7,
          1, 2, 8,
          1, 2, 9
        )
        check(Generator.from[IndexedSeq, Int](0 until 10).flatMap(x => Seq(1, 2, x)), expected)
        check(
          Generator.from[IndexedSeq, Int](0 until 10).flatMap(x => Generator.from(Seq(1, 2, x))),
          expected
        )
      }
      test("collect"){
        check(
          Generator.from[IndexedSeq, Int](0 until 10).collect{case k if k % 2 == 0 => k * k},
          Seq(0, 4, 16, 36, 64)
        )
      }
      test("collectFirst"){
        Generator.from[IndexedSeq, Int](0 until 10).collectFirst{case k if k > 5 => k * k} ==> Some(36)
        Generator.from[IndexedSeq, Int](0 until 10).collectFirst{case k if k > 15 => k * k} ==> None
      }
      test("slice"){
        check(Generator.from[IndexedSeq, Int](0 until 10).slice(3, 7), 3 until 7)
        check(Generator.from[IndexedSeq, Int](0 until 10).take(3), 0 until 3)
        check(Generator.from[IndexedSeq, Int](0 until 10).take(0), 0 until 0)
        check(Generator.from[IndexedSeq, Int](0 until 10).take(999), 0 until 10)
        check(Generator.from[IndexedSeq, Int](0 until 10).drop(3), 3 until 10)
        check(Generator.from[IndexedSeq, Int](0 until 10).drop(-1), 0 until 10)
      }
      test("takeWhile"){
        check(Generator.from[IndexedSeq, Int](0 until 10).takeWhile(_ < 5), 0 until 5)
      }
      test("dropWhile"){
        check(Generator.from[IndexedSeq, Int](0 until 10).dropWhile(_ < 5), 5 until 10)
      }
      test("zipWithIndex"){
        check(
          Generator.from[IndexedSeq, Int](5 until 10).zipWithIndex,
          Seq(
            5 -> 0,
            6 -> 1,
            7 -> 2,
            8 -> 3,
            9 -> 4
          )
        )
      }
      test("zip"){
        test("simple") - check(
          Generator.from[IndexedSeq, Int](0 until 5).zip(Seq('a', 'b', 'c', 'd', 'e')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
        test("truncateIfGenLonger") - check(
          Generator.from[IndexedSeq, Int](0 until 99).zip(Seq('a', 'b', 'c', 'd', 'e')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
        test("truncateIfIterableLonger") - check(
          Generator.from[IndexedSeq, Int](0 until 5).zip(Seq('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
      }
      test("head"){
        assert(
          Generator.from[IndexedSeq, Int](0 until 10).head == 0,
          Generator.from[IndexedSeq, Int](5 until 10).head == 5
        )
      }
      test("headOption"){
        assert(
          Generator.from[IndexedSeq, Int](0 until 10).headOption == Some(0),
          Generator.from[IndexedSeq, Int](5 until 10).headOption == Some(5),
          Generator.from[IndexedSeq, Int](0 until 0).headOption == None
        )
      }
      test("conversions"){
        assert(
          Generator.from[IndexedSeq, Int](0 until 10).toSeq == (0 until 10),
          Generator.from[IndexedSeq, Int](0 until 10).toVector == (0 until 10),
          Generator.from[IndexedSeq, Int](0 until 10).toArray.toSeq == (0 until 10),
          Generator.from[IndexedSeq, Int](0 until 10).toVector == (0 until 10),
          Generator.from[IndexedSeq, Int](0 until 10).toList == (0 until 10),
          Generator.from[IndexedSeq, Int](0 until 10).toSet == (0 until 10).toSet
        )
      }
      test("mkString"){
        assert(
          Generator.from[IndexedSeq, Int](0 until 10).mkString == "0123456789",
          Generator.from[IndexedSeq, Int](0 until 10).mkString(" ") == "0 1 2 3 4 5 6 7 8 9",
          Generator.from[IndexedSeq, Int](0 until 10).mkString("[", ", ", "]") == "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]"
        )
      }
      test("aggregates"){
        assert(
          Generator.from[IndexedSeq, Int](0 until 10).min == 0,
          Generator.from[IndexedSeq, Int](0 until 10).max == 9,
          Generator.from[IndexedSeq, Int](0 until 10).maxBy(x => math.abs(3 - x)) == 9,
          Generator.from[IndexedSeq, Int](0 until 10).minBy(x => math.abs(3 - x)) == 3,
          Generator.from[IndexedSeq, Int](0 until 10).sum == 45,
          Generator.from[IndexedSeq, Int](1 until 10).product == 362880,
          Generator.from[IndexedSeq, Int](0 until 10).reduce(_ + _) == 45,
          Generator.from[IndexedSeq, Int](0 until 10).fold(0)(_ + _) == 45
        )
      }
      test("creation"){
        val range = 0 until 10
        val seq = range.toSeq
        val iterator = range.toIterator
        val vector = range.toVector
        val array = range.toArray
        val set = range.toSet
        assert(
          (range: Generator[Int]).sum == 45,
          (seq: Generator[Int]).sum == 45,
          (iterator: Generator[Int]).sum == 45,
          (vector: Generator[Int]).sum == 45,
          (array: Generator[Int]).sum == 45,
          (set: Generator[Int]).sum == 45
        )
      }
      test("selfClosing"){
        var openSources = 0
        class DummyCloseableSource{
          val iterator = Iterator(1, 2, 3, 4, 5, 6, 7, 8, 9)
          openSources += 1
          var closed = false
          def close() = {
            closed = true
            openSources -= 1
          }
        }

        val g = Generator.selfClosing{
          val closeable = new DummyCloseableSource()
          (closeable.iterator, () => closeable.close())
        }
        // Make sure drop and take do not result in the source
        // not getting closed
        for(i <- 0 until 100) g.drop(1).take(2).toSeq

        // Even if the foreach fails in the middle with an exception,
        // the source must get closed
        intercept[Exception]{
          g.foreach{ z =>
            if (z == 5) throw new Exception()
          }
        }

        assert(openSources == 0)
      }
    }
    test("laziness"){
      var count = 0
      val taken = Generator.from[IndexedSeq, Int](0 until 10).map{ x => count += 1; x + 1}.take(3)
      // Before evaluation, nothing has happened!
      assert(count == 0)
      val seqed = taken.toSeq
      // After evaluation, the number of items evaluated is limited by the take call.
      assert(count == 3)
      assert(seqed == Seq(1, 2, 3))

      // After multiple evaluations, the count keeps going up since it's not memoized
      val seqed2 = taken.toSeq
      assert(count == 6)
      assert(seqed2 == Seq(1, 2, 3))
    }
    test("combinations"){
      def check[V](mkGen: Generator[Int] => Generator[V], mkSeq: Seq[Int] => Seq[V]) = {
        val seq = 0 until 10
        val gen = Generator.from[IndexedSeq, Int](seq)
        val seq1 = mkGen(gen).toSeq
        val seq2 = mkSeq(seq)
        assert(seq1 == seq2)
        seq1
      }
      test{
        check(
          _.filter(_ % 2 == 0).map(_ * 2),
          _.filter(_ % 2 == 0).map(_ * 2)
        )
      }
      test{
        check(
          _.filter(_ % 2 == 0).map(_ * 2).take(2),
          _.filter(_ % 2 == 0).map(_ * 2).take(2)
        )
      }
      test{
        check(
          _.filter(_ % 2 == 0).map(_ * 2).drop(2),
          _.filter(_ % 2 == 0).map(_ * 2).drop(2)
        )
      }
      test{
        check(
          _.filter(_ % 2 == 0).map(_ * 2).drop(2).drop(1),
          _.filter(_ % 2 == 0).map(_ * 2).drop(2).drop(1)
        )
      }
      test{
        check(
          _.filter(_ % 2 == 0).map(_.toString),
          _.filter(_ % 2 == 0).map(_.toString)
        )
      }
      test{
        check(
          _.filter(_ % 3 == 0).flatMap(x => Generator.from[IndexedSeq, Int](0 until x)).drop(2).take(9).flatMap(x => Generator.from[IndexedSeq, Int](0 until x)).slice(2, 8),
          _.filter(_ % 3 == 0).flatMap(0 until _).drop(2).take(9).flatMap(0 until _).slice(2, 8)
        )
      }
      test{
        check(
          _.flatMap(i => i.toString.toSeq).takeWhile(_ != '6').zipWithIndex.filter(_._1 != '2'),
          _.flatMap(i => i.toString.toSeq).takeWhile(_ != '6').zipWithIndex.filter(_._1 != '2')
        )
      }

      test{
        check(
          x => x.filter(_ % 2 == 0).map(_ * 2).drop(2) ++ x.map(_.toString.toSeq).flatMap(x => x),
          x => x.filter(_ % 2 == 0).map(_ * 2).drop(2) ++ x.map(_.toString.toSeq).flatMap(x => x)
        )
      }

    }
  }
}
