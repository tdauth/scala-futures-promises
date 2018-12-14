package tdauth.futuresandpromises.nonderived

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractExecutorTest, Executor}
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor

import scala.concurrent.ExecutionContext

class ExecutorTest extends AbstractExecutorTest {
  override def getTestName: String = "NonDerivedExecutor"
  override def getExecutor: Executor = executor

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}
