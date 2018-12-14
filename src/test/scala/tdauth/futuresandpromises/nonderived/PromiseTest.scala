package tdauth.futuresandpromises.nonderived

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractPromiseTest, Promise}

import scala.concurrent.ExecutionContext

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "NonDerivedPromise"
  override def getPromise: Promise[Int] = new NonDerivedPromise[Int](executor)

  private val executor = new NonDerivedExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}
