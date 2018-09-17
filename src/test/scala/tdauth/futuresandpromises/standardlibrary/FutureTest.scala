package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util

class FutureTest extends AbstractFutureTest {
  def getExecutor: Executor = new ScalaFPExecutor
  def getUtil: Util = new ScalaFPUtil
}