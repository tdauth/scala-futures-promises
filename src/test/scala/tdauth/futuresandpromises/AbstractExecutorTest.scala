package tdauth.futuresandpromises

import scala.concurrent.SyncVar

abstract class AbstractExecutorTest extends AbstractUnitSpec {
  getTestName should "call a function asynchronously" in {
    val executor = getExecutor
    val v = new SyncVar[Int]

    executor.submit(() => {
      v.put(1)
    })

    val r = v.take
    r should be(1)
  }

  def getExecutor: Executor

}