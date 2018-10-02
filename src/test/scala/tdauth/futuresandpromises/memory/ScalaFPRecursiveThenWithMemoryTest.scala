package tdauth.futuresandpromises.memory

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractRecursiveThenWithMemoryTest
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPFactory

class ScalaFPRecursiveThenWithMemoryTest extends AbstractRecursiveThenWithMemoryTest {
  val factory = new ScalaFPFactory
  val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))

  override def getTestName: String = "ScalaFPRecursiveThenWithMemoryTest"

  override def getSuccessfulFuture: Future[Int] = {
    val p = factory.createPromise[Int](executor)
    p.trySuccess(1)
    p.future()
  }
}