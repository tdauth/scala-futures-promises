package tdauth.futuresandpromises

abstract class AbstractFutureTest extends AbstractUnitSpec {
  getTestName should "be created asynchronously" in {
    val p = getPromise
    val future = p.future()
    future.isReady should be(false)
    p.trySuccess(10)
    future.get should be(10)
    future.isReady should be(true)
  }

  it should "get its callback be called asynchronously" in {
    val p = getPromise
    val future = p.future.then(t => t.get() + 10)
    p.trySuccess(10)
    future.get should be(20)
  }

  it should "be guarded by throwing the exception PredicateNotFulfilled" in {
    val p = getPromise
    val future = p.future.guard(_ != 10)
    p.trySuccess(10)
    the[PredicateNotFulfilled] thrownBy future.get should have message null
  }

  it should "be guarded successfully by not throwing any exception" in {
    val p = getPromise
    val future = p.future.guard(_ == 10)
    p.trySuccess(10)
    future.get should be(10)
  }

  it should "be guarded by throwing the initial exception" in {
    val p = getPromise
    val future = p.future.guard(_ == 10)
    p.tryFailure(new RuntimeException("test"))
    the[RuntimeException] thrownBy future.get should have message "test"
  }

  it should "complete the final future with first one over the second one with the help of orElse" in {
    val p0 = getPromise
    val f0 = p0.future
    val p1 = getPromise
    val f1 = p1.future()
    val f = f0.orElse(f1)
    p0.trySuccess(10)
    p1.trySuccess(11)
    f.get should be(10)

  }

  it should "complete the final future with the second one over the first one with the help of orElse" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.trySuccess(11)
    val f = f0.orElse(f1)
    f.get should be(11)
  }

  it should "complete the final future with the first one over the second one with the help of orElse when both are failing" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test 0"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.tryFailure(new RuntimeException("test 1"))
    val f = f0.orElse(f1)
    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  "first" should "complete the final future with the first one" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.trySuccess(10)
    val p1 = getPromise
    val f1 = p1.future()
    val f = f0.first(f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val p1 = getPromise
    val f1 = p1.future()
    p1.trySuccess(11)
    val p0 = getPromise
    val f0 = p0.future()
    val f = f0.first(f1)
    f.get should be(11)
  }

  it should "complete the final future with the second one although it fails" in {
    val p1 = getPromise
    val f1 = p1.future()
    p1.tryFailure(new RuntimeException("test 1"))
    val p0 = getPromise
    val f0 = p0.future
    val f = f0.first(f1)
    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  "firstSucc" should "complete the final future with the first one" in {
    val p0 = getPromise
    val f0 = p0.future
    p0.trySuccess(10)
    val p1 = getPromise
    val f1 = p1.future
    val f = f0.firstSucc(f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val p0 = getPromise
    val f0 = p0.future
    p0.tryFailure(new RuntimeException("test"))
    val p1 = getPromise
    val f1 = p1.future
    p1.trySuccess(11)
    val f = f0.firstSucc(f1)
    f.get should be(11)
  }

  it should "complete with the exception of the second future" in {
    val p0 = getPromise
    val f0 = p0.future
    p0.tryFailure(new RuntimeException("test 0"))
    val p1 = getPromise
    val f1 = p1.future
    val f = f0.firstSucc(f1)
    p1.tryFailure(new RuntimeException("test 1"))
    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  it should "complete with the exception of the first future" in {
    val p1 = getPromise
    val f1 = p1.future
    p1.tryFailure(new RuntimeException("test 1"))
    val p0 = getPromise
    val f0 = p0.future
    val f = f0.firstSucc(f1)
    p0.tryFailure(new RuntimeException("test 0"))
    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  def getPromise: Promise[Int]
}