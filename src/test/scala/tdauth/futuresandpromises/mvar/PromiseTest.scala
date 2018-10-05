package tdauth.futuresandpromises.mvar

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "MVarPromiseTest"
  override def getPromise: Promise[Int] = new MVarPromise[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}