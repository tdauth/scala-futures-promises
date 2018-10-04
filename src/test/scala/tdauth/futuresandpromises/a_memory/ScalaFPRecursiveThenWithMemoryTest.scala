package tdauth.futuresandpromises.a_memory

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractRecursiveThenWithMemoryTest
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPFactory

class ScalaFPRecursiveThenWithMemoryTest /* TODO extends #22 AbstractRecursiveThenWithMemoryTest */ {
  val factory = new ScalaFPFactory
  val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))

  /* TODO #22
  override def getTestName: String = "ScalaFPRecursiveThenWithMemoryTest"

  override def getSuccessfulFuture: Future[Int] = {
    val p = factory.createPromise[Int](executor)
    p.trySuccess(1)
    p.future()
  }
  */
}