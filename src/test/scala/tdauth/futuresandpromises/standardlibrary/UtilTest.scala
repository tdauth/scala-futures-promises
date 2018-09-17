package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  def getExecutor: Executor = new ScalaFPExecutor
  def getUtil: Util = new ScalaFPUtil
}