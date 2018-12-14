package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractFutureTest, Promise}

import scala.concurrent.ExecutionContext

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "ScalaFPFuture"
  override def getPromise: Promise[Int] = new ScalaFPPromise[Int](executor)

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}
