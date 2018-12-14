package tdauth.futuresandpromises.nonderived

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractFutureTest, Promise}

import scala.concurrent.ExecutionContext

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "NonDerivedFuture"
  override def getPromise: Promise[Int] = new NonDerivedPromise[Int](executor)

  private val executor = new NonDerivedExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}
