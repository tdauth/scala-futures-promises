package tdauth.futuresandpromises.stm

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "StmFutureTest"
  override def getPromise: Promise[Int] = new StmPromise[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}