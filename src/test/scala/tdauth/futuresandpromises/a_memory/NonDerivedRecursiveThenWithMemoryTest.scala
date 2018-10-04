package tdauth.futuresandpromises.a_memory

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractRecursiveThenWithMemoryTest
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.nonderived.NonDerivedExecutor
import tdauth.futuresandpromises.nonderived.NonDerivedFactory

class NonDerivedRecursiveThenWithMemoryTest /* TODO #22 extends AbstractRecursiveThenWithMemoryTest */ {
  val factory = new NonDerivedFactory
  val executor = new NonDerivedExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))

  /* TODO #22
  override def getTestName: String = "NonDerivedRecursiveThenWithMemoryTest"

  override def getSuccessfulFuture: Future[Int] = {
    val p = factory.createPromise[Int](executor)
    p.trySuccess(1)
    p.future()
  }
  */

}