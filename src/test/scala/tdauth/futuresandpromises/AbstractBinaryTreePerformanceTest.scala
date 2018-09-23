package tdauth.futuresandpromises

import org.scalameter.api.Bench

/**
 * Creates a binary tree with a certain height where each leaf is the call of a non-blocking combinator.
 */
abstract class AbstractBinaryTreePerformanceTest extends Bench.LocalTime {
  protected def getUtil: Util
  protected def getExecutor: Executor

  final val TREE_HEIGHT = 20

  protected type FutureType = Future[Int]

  protected def testCombinator(f: (FutureType, FutureType) => FutureType): Unit = {
    val r = testCombinatorRecursion(TREE_HEIGHT, f)
    r.sync
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

}