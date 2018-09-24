package tdauth.futuresandpromises.performance;

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractBinaryTreePerformanceTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPUtil

object FuturePerformanceTest extends AbstractBinaryTreePerformanceTest {
  override protected def getUtil: Util = new ScalaFPUtil
  override protected def getExecutor: Executor = new ScalaFPExecutor

  def getExecutor(n: Int) = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(n)))

  performance of "Future" in {
    measure method "orElse" in {
      using(CPU_RANGES) in {
        r => testCombinator(getExecutor(r.last), _ orElse _)
      }
    }

    measure method "first" in {
      using(CPU_RANGES) in {
        r => testCombinator(getExecutor(r.last), _ first _)
      }
    }

    measure method "firstSucc" in {
      using(CPU_RANGES) in {
        r => testCombinator(getExecutor(r.last), _ firstSucc _)
      }
    }

    measure method "firstN" in {
      using(CPU_RANGES) in {
        r => testCombinatorFirstN(getExecutor(r.last))
      }
    }

    measure method "firstNSucc" in {
      using(CPU_RANGES) in {
        r => testCombinatorFirstNSucc(getExecutor(r.last))
      }
    }
  }
}
