package tdauth.futuresandpromises.twitter

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractExecutorTest
import tdauth.futuresandpromises.Executor

class ExecutorTest extends AbstractExecutorTest {
  override def getTestName: String = "TwitterExecutor"
  override def getExecutor: Executor = executor

  private val executor = new TwitterExecutor(Executors.newSingleThreadExecutor())
}