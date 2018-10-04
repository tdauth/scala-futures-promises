package tdauth.futuresandpromises.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractExecutorTest
import tdauth.futuresandpromises.Executor

class ExecutorTest extends AbstractExecutorTest {
  override def getTestName: String = "CasExecutor"
  override def getExecutor: Executor = executor

  private val executor = new CasExecutor(Executors.newSingleThreadExecutor())
}