package tdauth.futuresandpromises

import scala.concurrent.SyncVar

abstract class AbstractExecutorTest extends AbstractUnitSpec {
  "An executor" should "call a function asynchronously" in {
    val executor = getExecutor
    val v = new SyncVar[Int]
    v.put(0)

    executor.submit(() => {
      delay()
      v.put(1)
    })
    val r0 = v.take
    r0 should be(0)

    delay()

    val r1 = v.take
    r1 should be(1)
  }

  def getExecutor: Executor

}