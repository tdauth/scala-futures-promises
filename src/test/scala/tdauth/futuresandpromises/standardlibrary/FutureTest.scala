package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.Promise

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "ScalaFPFuture"
  override def getPromise: Promise[Int] = new ScalaFPPromise[Int](executor)

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}