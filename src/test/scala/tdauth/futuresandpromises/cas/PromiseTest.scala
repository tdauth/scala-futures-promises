package tdauth.futuresandpromises.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "CasPromise"
  override def getPromise: Promise[Int] = new CasPromise[Int](executor)

  private val executor = new CasExecutor(Executors.newSingleThreadExecutor())
}