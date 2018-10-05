package tdauth.futuresandpromises.stm

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "StmPromiseTest"
  override def getPromise: Promise[Int] = new StmPromise[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}