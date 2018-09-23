package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractExecutorTest
import tdauth.futuresandpromises.Executor

class ExecutorTest extends AbstractExecutorTest {
  override def getTestName: String = "ScalaFPExecutor"
  override def getExecutor: Executor = new ScalaFPExecutor
}