package tdauth.futuresandpromises.mvar

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "MVarFutureTest"
  override def getPromise: Promise[Int] = new MVarPromise[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}