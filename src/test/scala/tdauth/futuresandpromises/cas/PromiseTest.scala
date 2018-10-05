package tdauth.futuresandpromises.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "CasPromiseTest"
  override def getPromise: Promise[Int] = new CasPromise[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}