package geny
import utest._
object TestGen extends TestSuite{
  val tests = this{
    'toStrings{
      def check(g: Gen[Int], expected: String) = {
        assert(g.toString == expected)
      }
      check(Gen(0 until 3), "Gen(Range(0, 1, 2))")
      check(Gen(0 until 3).filter(_ > 2), "Gen(Range(0, 1, 2)).filter(<function1>)")
      check(Gen(0 until 3).map(_ + 2), "Gen(Range(0, 1, 2)).map(<function1>)")
      check(Gen(0 until 3).takeWhile(_ > 2), "Gen(Range(0, 1, 2)).takeWhile(<function1>)")
      check(Gen(0 until 3).dropWhile(_ < 2), "Gen(Range(0, 1, 2)).dropWhile(<function1>)")
    }
    'unit{
      def check[T](gen: Gen[T], expected: Seq[T]) = {
        val toSeq = gen.toSeq
        assert(toSeq == expected)
      }
      'toSeq - check(Gen(0 until 10), 0 until 10)

      'filter - check(Gen(0 until 10).filter(_ > 5), 6 until 10)
      'map - {
        check(Gen(0 until 10).map(_ + 1), 1 until 11)
        check(
          Gen(0 until 10).map(i => i.toString * i),
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
        check(Gen(0 until 10).flatMap(x => Seq(1, 2, x)), expected)
        check(Gen(0 until 10).flatMap(x => Gen(Seq(1, 2, x))), expected)
      }
      'slice{
        check(Gen(0 until 10).slice(3, 7), 3 until 7)
        check(Gen(0 until 10).take(3), 0 until 3)
        check(Gen(0 until 10).drop(3), 3 until 10)
      }
      'takeWhile- check(Gen(0 until 10).takeWhile(_ < 5), 0 until 5)
      'dropWhile - check(Gen(0 until 10).dropWhile(_ < 5), 5 until 10)
      'zipWithIndex - check(
        Gen(5 until 10).zipWithIndex,
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
          Gen(0 until 5).zip(Seq('a', 'b', 'c', 'd', 'e')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
        'truncateIfGenLonger - check(
          Gen(0 until 99).zip(Seq('a', 'b', 'c', 'd', 'e')),
          Seq(
            0 -> 'a',
            1 -> 'b',
            2 -> 'c',
            3 -> 'd',
            4 -> 'e'
          )
        )
        'truncateIfIterableLonger - check(
          Gen(0 until 5).zip(Seq('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i')),
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
          Gen(0 until 10).head == 0,
          Gen(5 until 10).head == 5
        )
      }
      'conversions{
        assert(
          Gen(0 until 10).toSeq == (0 until 10),
          Gen(0 until 10).toVector == (0 until 10),
          Gen(0 until 10).toArray.toSeq == (0 until 10),
          Gen(0 until 10).toVector == (0 until 10),
          Gen(0 until 10).toList == (0 until 10),
          Gen(0 until 10).toSet == (0 until 10).toSet
        )
      }
      'mkString{
        assert(
          Gen(0 until 10).mkString == "0123456789",
          Gen(0 until 10).mkString(" ") == "0 1 2 3 4 5 6 7 8 9",
          Gen(0 until 10).mkString("[", ", ", "]") == "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]"
        )
      }


    }
    'combinations{
      def check[V](mkGen: Gen[Int] => Gen[V], mkSeq: Seq[Int] => Seq[V]) = {
        val seq = 0 until 10
        val gen = Gen(seq)
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
    }
  }
}
