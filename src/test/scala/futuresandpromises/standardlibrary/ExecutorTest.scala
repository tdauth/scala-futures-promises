package test.scala.futuresandpromises.standardlibrary

import org.scalatest._
import main.scala.futuresandpromises.standardlibrary.ScalaFPExecutor
import scala.concurrent.SyncVar

class ExecutorTest extends FlatSpec with Matchers {
  "A function" should "be called asynchronously" in {
    val executor = new ScalaFPExecutor
    val v = new SyncVar[Int]
    executor.submit(() => v.put(1))
    val r = v.get
    r should be(1)
  }
}