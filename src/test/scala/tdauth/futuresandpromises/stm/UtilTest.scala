package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  def getExecutor: Executor = new StmExecutor
  def getUtil: Util = new StmUtil
}