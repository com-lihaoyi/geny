package geny
import utest._
object TestGenerator extends TestSuite{
  val tests = this{
    'toStrings{
      def check(g: Generator[Int], expected: String) = {
        assert(g.toString == expected)
      }
      check(Generator(0, 1, 2), "Generator(WrappedArray(0, 1, 2))")
      check(Generator.fromIterable(0 until 3), "Generator(Range(0, 1, 2))")
      check(Generator.fromIterable(0 until 3).filter(_ > 2), "Generator(Range(0, 1, 2)).filter(<function1>)")
      check(Generator.fromIterable(0 until 3).map(_ + 2), "Generator(Range(0, 1, 2)).map(<function1>)")
      check(Generator.fromIterable(0 until 3).takeWhile(_ > 2), "Generator(Range(0, 1, 2)).takeWhile(<function1>)")
      check(Generator.fromIterable(0 until 3).dropWhile(_ < 2), "Generator(Range(0, 1, 2)).dropWhile(<function1>)")
    }
    'unit{
      def check[T](gen: Generator[T], expected: Seq[T]) = {
        val toSeq = gen.toSeq
        assert(toSeq == expected)
      }
      'toSeq - check(Generator.fromIterable(0 until 10), 0 until 10)

      'find - {
        assert(Generator.fromIterable(0 until 10).find(_ % 5 == 4) == Some(4))
        assert(Generator.fromIterable(0 until 10).find(_ % 100 == 40) == None)
      }
      'exists{
        assert(Generator.fromIterable(0 until 10).exists(_ == 4) == true)
        assert(Generator.fromIterable(0 until 10).exists(_ == 40) == false)
      }
      'contains{
        assert(Generator.fromIterable(0 until 10).contains(4) == true)
        assert(Generator.fromIterable(0 until 10).contains(40) == false)
      }
      'forAll{
        assert(Generator.fromIterable(0 until 10).forall(_  < 100) == true)
        assert(Generator.fromIterable(0 until 10).forall(_ < 5) == false)
      }
      'count{
        assert(Generator.fromIterable(0 until 10).count(_  < 100) == 10)
        assert(Generator.fromIterable(0 until 10).count(_  > 100) == 0)
        assert(Generator.fromIterable(0 until 10).count(_ < 5) == 5)
      }

      'reduceLeft{
        assert(Generator.fromIterable(0 until 10).reduceLeft(_ + _) == 45)
        intercept[UnsupportedOperationException](
          Generator.fromIterable(0 until 0).reduceLeft(_ + _)
        )
      }
      'foldLeft{
        assert(Generator.fromIterable(0 until 10).foldLeft(0)(_ + _) == 45)
        assert(Generator.fromIterable(0 until 0).foldLeft(0)(_ + _) == 0)
      }


      'concat{
        check(
          Generator.fromIterable(0 until 10) ++ Generator.fromIterable(0 until 10),
          (0 until 10) ++ (0 until 10)
        )
        check(
          Generator.fromIterable(0 until 10) ++ Generator.fromIterable(10 until 20),
          0 until 20
        )
      }
      'filter - check(Generator.fromIterable(0 until 10).filter(_ > 5), 6 until 10)
      'map - {
        check(Generator.fromIterable(0 until 10).map(_ + 1), 1 until 11)
        check(
          Generator.fromIterable(0 until 10).map(i => i.toString * i),
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
      'flatMap - {
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
        check(Generator.fromIterable(0 until 10).flatMap(x => Seq(1, 2, x)), expected)
        check(
          Generator.fromIterable(0 until 10).flatMap(x => Generator.fromIterable(Seq(1, 2, x))),
          expected
        )
      }
      'slice{
        check(Generator.fromIterable(0 until 10).slice(3, 7), 3 until 7)
        check(Generator.fromIterable(0 until 10).take(3), 0 until 3)
        check(Generator.fromIterable(0 until 10).take(0), 0 until 0)
        check(Generator.fromIterable(0 until 10).take(999), 0 until 10)
        check(Generator.fromIterable(0 until 10).drop(3), 3 until 10)
        check(Generator.fromIterable(0 until 10).drop(-1), 0 until 10)
      }
      'takeWhile- check(Generator.fromIterable(0 until 10).takeWhile(_ < 5), 0 until 5)
      'dropWhile - check(Generator.fromIterable(0 until 10).dropWhile(_ < 5), 5 until 10)
      'zipWithIndex - check(
        Generator.fromIterable(5 until 10).zipWithIndex,
        Seq(
          5 -> 0,
          6 -> 1,
          7 -> 2,
          8 -> 3,
          9 -> 4
        )
      )
      'zip{
        'simple - check(
          Generator.fromIterable(0 until 5).zip(Seq('a', 'b', 'c', 'd', 'e')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
        'truncateIfGenLonger - check(
          Generator.fromIterable(0 until 99).zip(Seq('a', 'b', 'c', 'd', 'e')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
        'truncateIfIterableLonger - check(
          Generator.fromIterable(0 until 5).zip(Seq('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
      }
      'head{
        assert(
          Generator.fromIterable(0 until 10).head == 0,
          Generator.fromIterable(5 until 10).head == 5
        )
      }
      'conversions{
        assert(
          Generator.fromIterable(0 until 10).toSeq == (0 until 10),
          Generator.fromIterable(0 until 10).toVector == (0 until 10),
          Generator.fromIterable(0 until 10).toArray.toSeq == (0 until 10),
          Generator.fromIterable(0 until 10).toVector == (0 until 10),
          Generator.fromIterable(0 until 10).toList == (0 until 10),
          Generator.fromIterable(0 until 10).toSet == (0 until 10).toSet
        )
      }
      'mkString{
        assert(
          Generator.fromIterable(0 until 10).mkString == "0123456789",
          Generator.fromIterable(0 until 10).mkString(" ") == "0 1 2 3 4 5 6 7 8 9",
          Generator.fromIterable(0 until 10).mkString("[", ", ", "]") == "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]"
        )
      }
      'selfClosing{
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
    'laziness{
      var count = 0
      val taken = Generator.fromIterable(0 until 10).map{ x => count += 1; x + 1}.take(3)
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
    'combinations{
      def check[V](mkGen: Generator[Int] => Generator[V], mkSeq: Seq[Int] => Seq[V]) = {
        val seq = 0 until 10
        val gen = Generator.fromIterable(seq)
        val seq1 = mkGen(gen).toSeq
        val seq2 = mkSeq(seq)
        assert(seq1 == seq2)
        seq1
      }
      * - check(
        _.filter(_ % 2 == 0).map(_ * 2),
        _.filter(_ % 2 == 0).map(_ * 2)
      )
      * - check(
        _.filter(_ % 2 == 0).map(_ * 2).take(2),
        _.filter(_ % 2 == 0).map(_ * 2).take(2)
      )
      * - check(
        _.filter(_ % 2 == 0).map(_ * 2).drop(2),
        _.filter(_ % 2 == 0).map(_ * 2).drop(2)
      )
      * - check(
        _.filter(_ % 2 == 0).map(_ * 2).drop(2).drop(1),
        _.filter(_ % 2 == 0).map(_ * 2).drop(2).drop(1)
      )
      * - check(
        _.filter(_ % 2 == 0).map(_.toString),
        _.filter(_ % 2 == 0).map(_.toString)
      )
      * - check(
        _.filter(_ % 3 == 0).flatMap(0 until _).drop(2).take(9).flatMap(0 until _).slice(2, 8),
        _.filter(_ % 3 == 0).flatMap(0 until _).drop(2).take(9).flatMap(0 until _).slice(2, 8)
      )
      * - check(
        _.flatMap(i => i.toString.toSeq).takeWhile(_ != '6').zipWithIndex.filter(_._1 != '2'),
        _.flatMap(i => i.toString.toSeq).takeWhile(_ != '6').zipWithIndex.filter(_._1 != '2')
      )

      * - check(
        x => x.filter(_ % 2 == 0).map(_ * 2).drop(2) ++ x.map(_.toString.toSeq).flatMap(x => x),
        x => x.filter(_ % 2 == 0).map(_ * 2).drop(2) ++ x.map(_.toString.toSeq).flatMap(x => x)
      )

    }
  }
}
