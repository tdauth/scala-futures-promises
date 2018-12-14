package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractPromiseTest, Promise}

import scala.concurrent.ExecutionContext

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "ScalaFPPromise"
  override def getPromise: Promise[Int] = new ScalaFPPromise[Int](executor)

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}
