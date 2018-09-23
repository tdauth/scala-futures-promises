package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.AbstractExecutorTest
import tdauth.futuresandpromises.Executor

class ExecutorTest extends AbstractExecutorTest {
  override def getTestName: String = "NonDerivedExecutor"
  override def getExecutor: Executor = new NonDerivedExecutor
}