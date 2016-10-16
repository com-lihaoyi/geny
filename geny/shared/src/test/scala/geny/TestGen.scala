package geny
import utest._
object TestGen extends TestSuite{
  val tests = this{
    'toStrings{
      def check(g: Gen[Int], expected: String) = {
        assert(g.toString == expected)
      }
      check(Gen(0, 1, 2), "Gen(WrappedArray(0, 1, 2))")
      check(Gen.fromIterable(0 until 3), "Gen(Range(0, 1, 2))")
      check(Gen.fromIterable(0 until 3).filter(_ > 2), "Gen(Range(0, 1, 2)).filter(<function1>)")
      check(Gen.fromIterable(0 until 3).map(_ + 2), "Gen(Range(0, 1, 2)).map(<function1>)")
      check(Gen.fromIterable(0 until 3).takeWhile(_ > 2), "Gen(Range(0, 1, 2)).takeWhile(<function1>)")
      check(Gen.fromIterable(0 until 3).dropWhile(_ < 2), "Gen(Range(0, 1, 2)).dropWhile(<function1>)")
    }
    'unit{
      def check[T](gen: Gen[T], expected: Seq[T]) = {
        val toSeq = gen.toSeq
        assert(toSeq == expected)
      }
      'toSeq - check(Gen.fromIterable(0 until 10), 0 until 10)

      'find - {
        assert(Gen.fromIterable(0 until 10).find(_ % 5 == 4) == Some(4))
        assert(Gen.fromIterable(0 until 10).find(_ % 100 == 40) == None)
      }
      'exists{
        assert(Gen.fromIterable(0 until 10).exists(_ == 4) == true)
        assert(Gen.fromIterable(0 until 10).exists(_ == 40) == false)
      }
      'contains{
        assert(Gen.fromIterable(0 until 10).contains(4) == true)
        assert(Gen.fromIterable(0 until 10).contains(40) == false)
      }
      'forAll{
        assert(Gen.fromIterable(0 until 10).forall(_  < 100) == true)
        assert(Gen.fromIterable(0 until 10).forall(_ < 5) == false)
      }
      'count{
        assert(Gen.fromIterable(0 until 10).count(_  < 100) == 10)
        assert(Gen.fromIterable(0 until 10).count(_  > 100) == 0)
        assert(Gen.fromIterable(0 until 10).count(_ < 5) == 5)
      }

      'reduceLeft{
        assert(Gen.fromIterable(0 until 10).reduceLeft(_ + _) == 45)
        intercept[UnsupportedOperationException](
          Gen.fromIterable(0 until 0).reduceLeft(_ + _)
        )
      }
      'foldLeft{
        assert(Gen.fromIterable(0 until 10).foldLeft(0)(_ + _) == 45)
        assert(Gen.fromIterable(0 until 0).foldLeft(0)(_ + _) == 0)
      }


      'concat{
        check(
          Gen.fromIterable(0 until 10) ++ Gen.fromIterable(0 until 10),
          (0 until 10) ++ (0 until 10)
        )
        check(
          Gen.fromIterable(0 until 10) ++ Gen.fromIterable(10 until 20),
          0 until 20
        )
      }
      'filter - check(Gen.fromIterable(0 until 10).filter(_ > 5), 6 until 10)
      'map - {
        check(Gen.fromIterable(0 until 10).map(_ + 1), 1 until 11)
        check(
          Gen.fromIterable(0 until 10).map(i => i.toString * i),
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
        check(Gen.fromIterable(0 until 10).flatMap(x => Seq(1, 2, x)), expected)
        check(
          Gen.fromIterable(0 until 10).flatMap(x => Gen.fromIterable(Seq(1, 2, x))),
          expected
        )
      }
      'slice{
        check(Gen.fromIterable(0 until 10).slice(3, 7), 3 until 7)
        check(Gen.fromIterable(0 until 10).take(3), 0 until 3)
        check(Gen.fromIterable(0 until 10).take(0), 0 until 0)
        check(Gen.fromIterable(0 until 10).take(999), 0 until 10)
        check(Gen.fromIterable(0 until 10).drop(3), 3 until 10)
        check(Gen.fromIterable(0 until 10).drop(-1), 0 until 10)
      }
      'takeWhile- check(Gen.fromIterable(0 until 10).takeWhile(_ < 5), 0 until 5)
      'dropWhile - check(Gen.fromIterable(0 until 10).dropWhile(_ < 5), 5 until 10)
      'zipWithIndex - check(
        Gen.fromIterable(5 until 10).zipWithIndex,
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
          Gen.fromIterable(0 until 5).zip(Seq('a', 'b', 'c', 'd', 'e')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
        'truncateIfGenLonger - check(
          Gen.fromIterable(0 until 99).zip(Seq('a', 'b', 'c', 'd', 'e')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
        'truncateIfIterableLonger - check(
          Gen.fromIterable(0 until 5).zip(Seq('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i')),
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
          Gen.fromIterable(0 until 10).head == 0,
          Gen.fromIterable(5 until 10).head == 5
        )
      }
      'conversions{
        assert(
          Gen.fromIterable(0 until 10).toSeq == (0 until 10),
          Gen.fromIterable(0 until 10).toVector == (0 until 10),
          Gen.fromIterable(0 until 10).toArray.toSeq == (0 until 10),
          Gen.fromIterable(0 until 10).toVector == (0 until 10),
          Gen.fromIterable(0 until 10).toList == (0 until 10),
          Gen.fromIterable(0 until 10).toSet == (0 until 10).toSet
        )
      }
      'mkString{
        assert(
          Gen.fromIterable(0 until 10).mkString == "0123456789",
          Gen.fromIterable(0 until 10).mkString(" ") == "0 1 2 3 4 5 6 7 8 9",
          Gen.fromIterable(0 until 10).mkString("[", ", ", "]") == "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]"
        )
      }
      'selfClosing{
        var openSources = 0
        class CloseableSource{
          val iterator = Iterator(1, 2, 3, 4, 5, 6, 7, 8, 9)
          openSources += 1
          var closed = false
          def close() = {
            closed = true
            openSources -= 1
          }
        }

        val g = Gen.selfClosing{
          val closeable = new CloseableSource()
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
      val taken = Gen.fromIterable(0 until 10).map{x => count += 1; x + 1}.take(3)
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
      def check[V](mkGen: Gen[Int] => Gen[V], mkSeq: Seq[Int] => Seq[V]) = {
        val seq = 0 until 10
        val gen = Gen.fromIterable(seq)
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
