package test.scala.futuresandpromises.standardlibrary

import org.scalatest._
import main.scala.futuresandpromises.standardlibrary.ScalaFPExecutor
import main.scala.futuresandpromises.standardlibrary.ScalaFPUtil

class FutureTest extends FlatSpec with Matchers {
  "A future" should "be created asynchronously" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10)
    future.get should be(10)
  }
}
