package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractExecutorTest
import tdauth.futuresandpromises.Executor

class ExecutorTest extends AbstractExecutorTest {
  def getExecutor: Executor = new ScalaFPExecutor
}