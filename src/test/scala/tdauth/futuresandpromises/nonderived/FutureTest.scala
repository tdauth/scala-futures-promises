package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "NonDerivedFuture"
  override def getExecutor: Executor = new NonDerivedExecutor
  override def getUtil: Util = new NonDerivedUtil
}