package tdauth.futuresandpromises

import scala.concurrent.SyncVar

abstract class AbstractFPTest extends AbstractUnitSpec {

  getTestName should "only print the test name" in {
  }

  // Basic future methods:
  "get " should "return a successful value" in {
    val p = getFP
    p.isReady should be(false)
    p.trySuccess(10)
    p.get should be(10)
    p.isReady should be(true)
  }

  // Basic promise methods:
  "tryComplete" should "not complete a future successfully" in {
    val p = getFP
    p.tryComplete(new Try[Int](10)) should be(true)
    p.get should be(10)
  }

  it should "not complete a future which is already completed" in {
    val p = getFP
    p.trySuccess(10) should be(true)
    p.tryComplete(new Try[Int](11)) should be(false)
    p.get should be(10)
  }

  // Derived promise methods:
  "trySuccess" should "should complete a future successfully" in {
    val p = getFP
    p.trySuccess(10) should be(true)
    p.get should be(10)
  }

  it should "not complete a future which is already completed" in {
    val p = getFP
    p.trySuccess(10) should be(true)
    p.trySuccess(11) should be(false)
    p.get should be(10)
  }

  "tryFail" should "complete a future with an exception" in {
    val p = getFP
    p.tryFail(new RuntimeException("test")) should be(true)
    the[RuntimeException] thrownBy p.get should have message "test"
  }

  it should "not complete a future which is already completed" in {
    val p = getFP
    p.tryFail(new RuntimeException("test")) should be(true)
    p.tryFail(new RuntimeException("test 2")) should be(false)
    the[RuntimeException] thrownBy p.get should have message "test"
  }

  "tryCompleteWith" should "complete a future with the help of another future" in {
    val p0 = getFP
    p0.trySuccess(10)

    val p = getFP
    p.tryCompleteWith(p0)
    p.get should be(10)
  }

  "trySuccessWith" should "complete a future successfully with the help of another future" in {
    val p0 = getFP
    p0.trySuccess(10)

    val p = getFP
    p.trySuccessWith(p0)
    p.get should be(10)
  }

  it should "not complete a future with the help of a failing future" in {
    val p0 = getFP
    p0.tryFail(new RuntimeException("test"))

    val p = getFP
    p.trySuccessWith(p0)
    p.isReady should be(false)
    p.trySuccess(11) should be(true)
    p.get should be(11)
  }

  "tryFailWith" should "complete a future with an exception with the help of another future" in {
    val p0 = getFP
    p0.tryFail(new RuntimeException("test"))

    val p = getFP
    p.tryFailWith(p0)
    the[RuntimeException] thrownBy p.get should have message "test"
  }

  it should "not complete a future with the help of a successful future" in {
    val p0 = getFP
    p0.trySuccess(10)

    val p = getFP
    p.tryFailWith(p0)
    p.isReady should be(false)
    p.tryFail(new RuntimeException("test")) should be(true)
    the[RuntimeException] thrownBy p.get should have message "test"
  }

  // Derived future methods:
  "future" should "complete a promise successfully" in {
    val p = getFP
    val f = p.future[String](() => "Hello world!")
    f.get should be("Hello world!")
  }

  it should "fail a future" in {
    val p = getFP
    val f = p.future[String](() => throw new RuntimeException("Failure!"))
    the[RuntimeException] thrownBy f.get should have message "Failure!"
  }

  "onSuccess" should "register a callback which is called" in {
    val p = getFP
    val s = new SyncVar[Int]
    p.onSuccess(v => s.put(v))
    p.trySuccess(10) should be(true)
    s.get should be(10)
  }

  "onFail" should "register a callback which is called" in {
    val p = getFP
    val s = new SyncVar[Throwable]
    p.onFail(v => s.put(v))
    p.tryFail(new RuntimeException("test")) should be(true)
    s.get.getMessage should be("test")
  }

  "transform" should "create a new successful future" in {
    val p = getFP
    val s = p.transform(v => v.get * 10)
    p.trySuccess(10)
    s.get should be(100)
  }

  it should "create a failed future" in {
    val p = getFP
    val s = p.transform(v => throw new RuntimeException("test"))
    p.trySuccess(10)
    the[RuntimeException] thrownBy s.get should have message "test"
  }

  "transformWith" should "create a new successful future" in {
    val p = getFP
    val p0 = getFP
    val s = p.transformWith(v => p0)
    p.trySuccess(10)
    p0.trySuccess(11)
    s.get should be(11)
  }

  it should "create a failed future" in {
    val p = getFP
    val p0 = getFP
    val s = p.transformWith(v => p0)
    p.trySuccess(10)
    p0.tryFail(new RuntimeException("test"))
    the[RuntimeException] thrownBy s.get should have message "test"
  }

  "followedBy" should "create a new successful future" in {
    val p = getFP
    val s = p.followedBy(_ * 10)
    p.trySuccess(10)
    s.get should be(100)
  }

  it should "create a failed future" in {
    val p = getFP
    val s = p.followedBy(v => throw new RuntimeException("test"))
    p.trySuccess(10)
    the[RuntimeException] thrownBy s.get should have message "test"
  }

  "followedByWith" should "create a new successful future" in {
    val p = getFP
    val p0 = getFP
    val s = p.followedByWith(v => p0)
    p.trySuccess(10)
    p0.trySuccess(11)
    s.get should be(11)
  }

  it should "create a failed future" in {
    val p = getFP
    val p0 = getFP
    val s = p.followedByWith(v => p0)
    p.trySuccess(10)
    p0.tryFail(new RuntimeException("test"))
    the[RuntimeException] thrownBy s.get should have message "test"
  }

  it should "create a failed future by the first one" in {
    val p = getFP
    val p0 = getFP
    val s = p.followedByWith(v => p0)
    p.tryFail(new RuntimeException("test 0"))
    p0.tryFail(new RuntimeException("test 1"))
    the[RuntimeException] thrownBy s.get should have message "test 0"
  }

  "guard" should "throw the exception PredicateNotFulfilled" in {
    val p = getFP
    val future = p.guard(_ != 10)
    p.trySuccess(10)
    the[PredicateNotFulfilled] thrownBy future.get should have message null
  }

  it should "not throw any exception" in {
    val p = getFP
    val future = p.guard(_ == 10)
    p.trySuccess(10)
    future.get should be(10)
  }

  it should "throw the initial exception" in {
    val p = getFP
    val future = p.guard(_ == 10)
    p.tryFail(new RuntimeException("test"))
    the[RuntimeException] thrownBy future.get should have message "test"
  }

  "orAlt" should "complete the final future with first one over the second one" in {
    val p0 = getFP
    val p1 = getFP
    val f = p0.orAlt(p1)
    p0.trySuccess(10)
    p1.trySuccess(11)
    f.get should be(10)

  }

  it should "complete the final future with the second one over the first one" in {
    val p0 = getFP
    p0.tryFail(new RuntimeException("test"))
    val p1 = getFP
    p1.trySuccess(11)
    val f = p0.orAlt(p1)
    f.get should be(11)
  }

  it should "complete the final future with the first one over the second one when both are failing" in {
    val p0 = getFP
    p0.tryFail(new RuntimeException("test 0"))
    val p1 = getFP
    p1.tryFail(new RuntimeException("test 1"))
    val f = p0.orAlt(p1)
    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  "first" should "complete the final future with the first one" in {
    val p0 = getFP
    p0.trySuccess(10)
    val p1 = getFP
    val f = p0.first(p1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val p1 = getFP
    p1.trySuccess(11)
    val p0 = getFP
    val f = p1.first(p0)
    f.get should be(11)
  }

  it should "complete the final future with the second one although it fails" in {
    val p1 = getFP
    p1.tryFail(new RuntimeException("test 1"))
    val p0 = getFP
    val f = p0.first(p1)
    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  "firstSucc" should "complete the final future with the first one" in {
    val p0 = getFP
    p0.trySuccess(10)
    val p1 = getFP
    val f = p0.firstSucc(p1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val p0 = getFP
    p0.tryFail(new RuntimeException("test"))
    the[RuntimeException] thrownBy p0.get should have message "test"
    val p1 = getFP
    p1.trySuccess(11)
    val f = p0.firstSucc(p1)
    f.get should be(11)
  }

  it should "complete with the exception of the second future" in {
    val p0 = getFP
    p0.tryFail(new RuntimeException("test 0"))
    the[RuntimeException] thrownBy p0.get should have message "test 0"
    val p1 = getFP
    val f = p0.firstSucc(p1)
    p1.tryFail(new RuntimeException("test 1"))
    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  it should "complete with the exception of the first future" in {
    val p1 = getFP
    p1.tryFail(new RuntimeException("test 1"))
    the[RuntimeException] thrownBy p1.get should have message "test 1"
    val p0 = getFP
    val f = p0.firstSucc(p1)
    p0.tryFail(new RuntimeException("test 0"))
    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  def getFP: FP[Int]
}