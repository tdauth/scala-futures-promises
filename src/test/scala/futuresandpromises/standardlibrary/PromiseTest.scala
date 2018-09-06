package test.scala.futuresandpromises.standardlibrary

import org.scalatest._
import main.scala.futuresandpromises.standardlibrary.ScalaFPExecutor
import main.scala.futuresandpromises.standardlibrary.ScalaFPPromise
import main.scala.futuresandpromises.standardlibrary.ScalaFPTry
import main.scala.futuresandpromises.standardlibrary.ScalaFPUtil
import main.scala.futuresandpromises.PredicateNotFulfilled
import main.scala.futuresandpromises.UsingUninitializedTry

class PromiseTest extends FlatSpec with Matchers {
  "A future" should "be completed successfully by a promise" in {
    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.trySuccess(10)
    f.get should be(10)
  }

  "A future" should "be completed by an exception by a promise" in {
    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.tryFailure(new RuntimeException("test"))
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  "A future" should "be completed by a promise with an empty Try" in {
    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.tryComplete(new ScalaFPTry)
    the[UsingUninitializedTry] thrownBy f.get should have message null
  }

  "A future" should "be completed by a promise with the help of a future" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10)

    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.tryCompleteWith(future)
    f.get should be(10)
  }

  "A future" should "be completed successfully by a promise with the help of a future" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10)

    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.trySuccessWith(future)
    f.get should be(10)
  }

  "A future" should "be completed by an exception by a promise with the help of a future" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test"))

    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.tryFailureWith(future)
    the[RuntimeException] thrownBy f.get should have message "test"
  }
}
