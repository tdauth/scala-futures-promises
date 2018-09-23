package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "ScalaFPFuture"
  override def getExecutor: Executor = new ScalaFPExecutor
  override def getUtil: Util = new ScalaFPUtil
}