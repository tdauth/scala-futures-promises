package tdauth.futuresandpromises

abstract class AbstractPromiseTest extends AbstractUnitSpec {
  getTestName should "produce an empty future" in {
    val p = getPromise[Int]
    val f = p.future()
    f.isReady should be(false)
  }

  "trySuccess" should "should complete a future successfully" in {
    val p = getPromise[Int]
    val f = p.future()
    p.trySuccess(10)
    f.get should be(10)
  }

  "tryFailure" should "complete a future with an exception" in {
    val p = getPromise[Int]
    val f = p.future()
    p.tryFailure(new RuntimeException("test"))
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  "tryComplete" should "complete a future with an an empty Try" in {
    val p = getPromise[Int]
    val f = p.future()
    p.tryComplete(getTry[Int])
    the[UsingUninitializedTry] thrownBy f.get should have message null
  }

  "tryCompleteWith" should "complete a future with the help of another future" in {
    val executor = getExecutor
    val future = getUtil.async(executor, () => 10)

    val p = getPromise[Int]
    val f = p.future()
    p.tryCompleteWith(future)
    f.get should be(10)
  }

  "trySuccessWith" should "complete a future successfully with the help of another future" in {
    val executor = getExecutor
    val future = getUtil.async(executor, () => 10)

    val p = getPromise[Int]
    val f = p.future()
    p.trySuccessWith(future)
    f.get should be(10)
  }

  it should "not complete a future with the help of a failing future" in {
    val executor = getExecutor
    val future = getUtil.async[Int](executor, () => throw new RuntimeException("test"))

    val p = getPromise[Int]
    val f = p.future()
    p.trySuccessWith(future)
    future.sync
    f.isReady should be(false)
    p.trySuccess(11) should be(true)
    f.get should be(11)
  }

  "tryFailureWith" should "complete a future with an exception with the help of another future" in {
    val executor = getExecutor
    val future = getUtil.async[Int](executor, () => throw new RuntimeException("test"))

    val p = getPromise[Int]
    val f = p.future()
    p.tryFailureWith(future)
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  it should "not complete a future with the help of a successful future" in {
    val executor = getExecutor
    val future = getUtil.async(executor, () => 10)

    val p = getPromise[Int]
    val f = p.future()
    p.tryFailureWith(future)
    future.sync
    f.isReady should be(false)
    p.tryFailure(new RuntimeException("test")) should be(true)
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  def getExecutor: Executor
  def getUtil: Util
  def getPromise[T]: Promise[T]
  def getTry[T]: Try[T]
}