package tdauth.futuresandpromises.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "CasFutureTest"
  override def getPromise: Promise[Int] = new CasPromise[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}