package tdauth.futuresandpromises.nonderived

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.Promise

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "NonDerivedFuture"
  override def getPromise: Promise[Int] = new NonDerivedPromise[Int](executor)

  private val executor = new NonDerivedExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}