package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "NonDerivedUtil"
  override def getExecutor: Executor = new NonDerivedExecutor
  override def getUtil: Util = new NonDerivedUtil
}