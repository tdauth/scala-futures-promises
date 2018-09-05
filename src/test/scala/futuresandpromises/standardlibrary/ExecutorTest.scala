package test.scala.futuresandpromises.standardlibrary

import org.scalatest._

class ExecutorTest extends FlatSpec with Matchers {
   "A function" should "be called asynchronously" in {
    val executor = new ScalaFPExecutor
    var i : Integer = 0
    executor.submit(() => i + 1)
    i should be 1
  }
}