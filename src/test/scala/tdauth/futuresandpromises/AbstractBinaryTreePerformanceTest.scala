package tdauth.futuresandpromises

import org.scalameter.api.Bench

/**
 * Creates a binary tree with a height of {@link #TREE_HEIGHT} where each leaf is the call of a non-blocking combinator with two futures.
 */
abstract class AbstractBinaryTreePerformanceTest extends Bench.LocalTime {
  protected def getUtil: Util
  protected def getExecutor: Executor

  final val TREE_HEIGHT = 20

  protected type FutureType = Future[Int]

  protected def testCombinator(f: (FutureType, FutureType) => FutureType): Unit = {
    val r = testCombinatorRecursion(TREE_HEIGHT, f)
    r.get
  }

  private def testCombinatorRecursion(level: Int, f: (FutureType, FutureType) => FutureType): FutureType = {
    if (level == 0) {
      val f0 = getUtil.async(getExecutor, () => 10)
      val f1 = getUtil.async(getExecutor, () => 11)

      f(f0, f1)
    } else {
      val f0 = testCombinatorRecursion(level - 1, f)
      val f1 = testCombinatorRecursion(level - 1, f)

      f(f0, f1)
    }
  }

  protected def testCombinatorFirstN(): Unit = {
    val r = testCombinatorFirstNRecursion(TREE_HEIGHT)
    r.get
  }

  private def testCombinatorFirstNRecursion(level: Int): Future[Util#FirstNResultType[Int]] = {
    if (level == 0) {
      val f0 = getUtil.async(getExecutor, () => 10)
      val f1 = getUtil.async(getExecutor, () => 11)

      getUtil.firstN(Vector(f0, f1), 2)
    } else {
      val f0 = testCombinatorFirstNRecursion(level - 1)
      val f1 = testCombinatorFirstNRecursion(level - 1)
      val callback = (t: Try[Util#FirstNResultType[Int]]) => t.get().apply(0)._2.get
      getUtil.firstN(Vector(f0.then(callback), f1.then(callback)), 2)
    }
  }

  protected def testCombinatorFirstNSucc(): Unit = {
    val r = testCombinatorFirstNSuccRecursion(TREE_HEIGHT)
    r.get
  }

  private def testCombinatorFirstNSuccRecursion(level: Int): Future[Util#FirstNSuccResultType[Int]] = {
    if (level == 0) {
      val f0 = getUtil.async(getExecutor, () => 10)
      val f1 = getUtil.async(getExecutor, () => 11)

      getUtil.firstNSucc(Vector(f0, f1), 2)
    } else {
      val f0 = testCombinatorFirstNSuccRecursion(level - 1)
      val f1 = testCombinatorFirstNSuccRecursion(level - 1)
      val callback = (t: Try[Util#FirstNSuccResultType[Int]]) => t.get().apply(0)._2
      getUtil.firstNSucc(Vector(f0.then(callback), f1.then(callback)), 2)
    }
  }

}