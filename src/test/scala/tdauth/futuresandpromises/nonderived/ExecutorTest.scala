package tdauth.futuresandpromises.nonderived

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractExecutorTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor

class ExecutorTest extends AbstractExecutorTest {
  override def getTestName: String = "NonDerivedExecutor"
  override def getExecutor: Executor = executor

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}