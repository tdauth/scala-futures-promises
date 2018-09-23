package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "ScalaFPUtil"
  override def getExecutor: Executor = new ScalaFPExecutor
  override def getUtil: Util = new ScalaFPUtil
}