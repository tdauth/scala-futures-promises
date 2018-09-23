package tdauth.futuresandpromises.performance;

import tdauth.futuresandpromises.AbstractBinaryTreePerformanceTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPUtil

class FuturePerformanceTest extends AbstractBinaryTreePerformanceTest {
  override protected def getUtil: Util = new ScalaFPUtil
  override protected def getExecutor: Executor = new ScalaFPExecutor

  performance of "Future" in {
    measure method "orElse" in {
      testCombinator(_ orElse _)
    }

    measure method "first" in {
      testCombinator(_ first _)
    }

    measure method "firstSucc" in {
      testCombinator(_ firstSucc _)
    }
  }
}
