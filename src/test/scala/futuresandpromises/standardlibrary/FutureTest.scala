package test.scala.futuresandpromises.standardlibrary

import org.scalatest._
import main.scala.futuresandpromises.standardlibrary.ScalaFPExecutor
import main.scala.futuresandpromises.standardlibrary.ScalaFPUtil
import main.scala.futuresandpromises.PredicateNotFulfilled

class FutureTest extends FlatSpec with Matchers {
  "A future" should "be created asynchronously" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10)
    future.get should be(10)
  }

  "The callback" should "be called asynchronously" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10).then(t => t.get() + 10)
    future.get should be(20)
  }

  "The guard" should "be throw the exception PredicateNotFulfilled" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10).guard(v => v != 10)
    the[PredicateNotFulfilled] thrownBy future.get should have message null
  }

  "The guard" should "not throw any exception" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10).guard(v => v == 10)
    future.get should be(10)
  }

  "The guard" should "throw the initial exception" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test")).guard(v => v == 10)
    the[RuntimeException] thrownBy future.get should have message "test"
  }
}
