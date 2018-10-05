package tdauth.futuresandpromises

abstract class AbstractPromiseTest extends AbstractUnitSpec {
  getTestName should "produce an empty future" in {
    val p = getPromise
    val f = p.future()
    f.isReady should be(false)
  }

  "trySuccess" should "should complete a future successfully" in {
    val p = getPromise
    val f = p.future()
    p.trySuccess(10) should be(true)
    f.get should be(10)
  }

  it should "not complete a future which is already completed" in {
    val p = getPromise
    val f = p.future()
    p.trySuccess(10) should be(true)
    p.trySuccess(11) should be(false)
    f.get should be(10)
  }

  "tryFailure" should "complete a future with an exception" in {
    val p = getPromise
    val f = p.future()
    p.tryFailure(new RuntimeException("test")) should be(true)
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  it should "not complete a future which is already completed" in {
    val p = getPromise
    val f = p.future()
    p.tryFailure(new RuntimeException("test")) should be(true)
    p.tryFailure(new RuntimeException("test 2")) should be(false)
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  "tryComplete" should "not complete a future successfully" in {
    val p = getPromise
    val f = p.future()
    p.tryComplete(new Try[Int](10)) should be(true)
    f.get should be(10)
  }

  it should "not complete a future which is already completed" in {
    val p = getPromise
    val f = p.future()
    p.trySuccess(10) should be(true)
    p.tryComplete(new Try[Int](11)) should be(false)
    f.get should be(10)
  }

  it should "complete a future with an an empty Try" in {
    val p = getPromise
    val f = p.future()
    p.tryComplete(new Try[Int]())
    the[UsingUninitializedTry] thrownBy f.get should have message null
  }

  "tryCompleteWith" should "complete a future with the help of another future" in {
    val p0 = getPromise
    val future = p0.future()
    p0.trySuccess(10)

    val p = getPromise
    val f = p.future()
    p.tryCompleteWith(future)
    f.get should be(10)
  }

  "trySuccessWith" should "complete a future successfully with the help of another future" in {
    val p0 = getPromise
    val future = p0.future()
    p0.trySuccess(10)

    val p = getPromise
    val f = p.future()
    p.trySuccessWith(future)
    f.get should be(10)
  }

  it should "not complete a future with the help of a failing future" in {
    val p0 = getPromise
    val future = p0.future()
    p0.tryFailure(new RuntimeException("test"))

    val p = getPromise
    val f = p.future()
    p.trySuccessWith(future)
    f.isReady should be(false)
    p.trySuccess(11) should be(true)
    f.get should be(11)
  }

  "tryFailureWith" should "complete a future with an exception with the help of another future" in {
    val p0 = getPromise
    val future = p0.future()
    p0.tryFailure(new RuntimeException("test"))

    val p = getPromise
    val f = p.future()
    p.tryFailureWith(future)
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  it should "not complete a future with the help of a successful future" in {
    val p0 = getPromise
    val future = p0.future()
    p0.trySuccess(10)

    val p = getPromise
    val f = p.future()
    p.tryFailureWith(future)
    f.isReady should be(false)
    p.tryFailure(new RuntimeException("test")) should be(true)
    the[RuntimeException] thrownBy f.get should have message "test"
  }

  def getPromise: Promise[Int]
}