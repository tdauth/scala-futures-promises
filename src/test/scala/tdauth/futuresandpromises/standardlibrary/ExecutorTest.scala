package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractExecutorTest
import tdauth.futuresandpromises.Executor

class ExecutorTest extends AbstractExecutorTest {
  override def getTestName: String = "ScalaFPExecutor"
  override def getExecutor: Executor = executor

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}