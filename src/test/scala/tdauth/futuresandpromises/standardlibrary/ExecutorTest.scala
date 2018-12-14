package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractExecutorTest, Executor}

import scala.concurrent.ExecutionContext

class ExecutorTest extends AbstractExecutorTest {
  override def getTestName: String = "ScalaFPExecutor"
  override def getExecutor: Executor = executor

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}
