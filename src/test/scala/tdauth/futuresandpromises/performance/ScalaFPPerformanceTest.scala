package tdauth.futuresandpromises.performance;

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractBinaryTreePerformanceTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPUtil

object ScalaFPPerformanceTest extends AbstractBinaryTreePerformanceTest {
  override protected def getUtil: Util = new ScalaFPUtil

  def getExecutor(n: Int) = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(n)))

  performance of "ScalaFPFuture" in {
    measure method "orElse" in {
      using(NUMBER_OF_THREADS) in {
        r => testCombinator(getExecutor(r), _ orElse _)
      }
    }

    measure method "first" in {
      using(NUMBER_OF_THREADS) in {
        r => testCombinator(getExecutor(r), _ first _)
      }
    }

    measure method "firstSucc" in {
      using(NUMBER_OF_THREADS) in {
        r => testCombinator(getExecutor(r), _ firstSucc _)
      }
    }

    measure method "firstN" in {
      using(NUMBER_OF_THREADS) in {
        r => testCombinatorFirstN(getExecutor(r))
      }
    }

    measure method "firstNSucc" in {
      using(NUMBER_OF_THREADS) in {
        r => testCombinatorFirstNSucc(getExecutor(r))
      }
    }
  }
}
