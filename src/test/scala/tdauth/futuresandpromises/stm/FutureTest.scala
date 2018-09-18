package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util
import tdauth.futuresandpromises.AbstractFutureTest

class FutureTest extends AbstractFutureTest {
  def getExecutor: Executor = new StmExecutor
  def getUtil: Util = new StmUtil
}