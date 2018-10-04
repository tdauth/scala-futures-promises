package tdauth.futuresandpromises.nonderived

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "NonDerivedPromise"
  override def getPromise: Promise[Int] = new NonDerivedPromise[Int](executor)

  private val executor = new NonDerivedExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}