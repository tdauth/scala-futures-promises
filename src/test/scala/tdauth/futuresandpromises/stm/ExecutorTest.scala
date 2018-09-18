package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.AbstractExecutorTest
import tdauth.futuresandpromises.Executor

class ExecutorTest extends AbstractExecutorTest {
  def getExecutor: Executor = new StmExecutor
}