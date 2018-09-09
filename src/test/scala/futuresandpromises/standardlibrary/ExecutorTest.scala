package tdauth.futuresandpromises.standardlibrary

import scala.concurrent.SyncVar

import tdauth.futuresandpromises.UnitSpec

class ExecutorTest extends UnitSpec {
  "An executor" should "call a function asynchronously" in {
    val executor = new ScalaFPExecutor
    val v = new SyncVar[Int]
    executor.submit(() => v.put(1))
    val r = v.get
    r should be(1)
  }
}