package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.UnitSpec
import tdauth.futuresandpromises.UsingUninitializedTry

class PromiseTest extends UnitSpec {
  "A promise" should "should complete a future successfully" in {
    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.trySuccess(10)
    f.get should be(10)
  }

  it should "complete a future with an exception" in {
    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.tryFailure(new RuntimeException("test"))
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  it should "complete a future with an an empty Try" in {
    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.tryComplete(new ScalaFPTry)
    the[UsingUninitializedTry] thrownBy f.get should have message null
  }

  it should "complete a future with the help of another future" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10)

    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.tryCompleteWith(future)
    f.get should be(10)
  }

  it should "complete a future successfully with the help of another future" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10)

    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.trySuccessWith(future)
    f.get should be(10)
  }

  it should "complete a future with an exception with the help of another future" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test"))

    val p = new ScalaFPPromise[Int]()
    val f = p.future()
    p.tryFailureWith(future)
    the[RuntimeException] thrownBy f.get should have message "test"
  }
}
