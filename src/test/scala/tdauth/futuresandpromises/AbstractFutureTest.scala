package tdauth.futuresandpromises

abstract class AbstractFutureTest extends AbstractUnitSpec {
  getTestName should "be created asynchronously" in {
    val executor = getExecutor
    val util = getUtil
    val future = util.async(executor, () => { delay(); 10 })
    future.isReady should be(false)
    future.get should be(10)
    future.isReady should be(true)
  }

  it should "get its callback be called asynchronously" in {
    val executor = getExecutor
    val util = getUtil
    val future = util.async(executor, () => 10).then(t => t.get() + 10)
    future.get should be(20)
  }

  it should "be guarded by throwing the exception PredicateNotFulfilled" in {
    val executor = getExecutor
    val util = getUtil
    val future = util.async(executor, () => 10).guard(v => v != 10)
    the[PredicateNotFulfilled] thrownBy future.get should have message null
  }

  it should "be guarded successfully by not throwing any exception" in {
    val executor = getExecutor
    val util = getUtil
    val future = util.async(executor, () => 10).guard(v => v == 10)
    future.get should be(10)
  }

  it should "be guarded by throwing the initial exception" in {
    val executor = getExecutor
    val util = getUtil
    val future = util.async[Int](executor, () => throw new RuntimeException("test")).guard(v => v == 10)
    the[RuntimeException] thrownBy future.get should have message "test"
  }

  it should "complete the final future with first one over the second one with the help of orElse" in {
    val executor = getExecutor
    val util = getUtil
    val f0 = util.async(executor, () => 10)
    f0.sync
    val f1 = util.async(executor, () => { delay(); 11 })
    val f = f0.orElse(f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one over the first one with the help of orElse" in {
    val executor = getExecutor
    val util = getUtil
    val f0 = util.async[Int](executor, () => throw new RuntimeException("test"))
    val f1 = util.async(executor, () => 11)
    val f = f0.orElse(f1)
    f.get should be(11)
  }

  it should "complete the final future with the first one over the second one with the help of orElse when both are failing" in {
    val executor = getExecutor
    val util = getUtil
    val f0 = util.async[Int](executor, () => throw new RuntimeException("test 0"))
    f0.sync
    val f1 = util.async[Int](executor, () => { delay(); throw new RuntimeException("test 1") })
    val f = f0.orElse(f1)
    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  "first" should "complete the final future with the first one" in {
    val executor = getExecutor
    val util = getUtil
    val f0 = util.async(executor, () => 10)
    f0.sync
    val f1 = util.async(executor, () => { delay(); 11 })
    val f = f0.first(f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val executor = getExecutor
    val util = getUtil
    val f1 = util.async(executor, () => 11)
    f1.sync
    val f0 = util.async(executor, () => { delay(); 10 })
    val f = f0.first(f1)
    f.get should be(11)
  }

  it should "complete the final future with the second one although it fails" in {
    val executor = getExecutor
    val util = getUtil
    val f1 = util.async[Int](executor, () => throw new RuntimeException("test 1"))
    f1.sync
    val f0 = util.async(executor, () => { delay(); 10 })
    val f = f0.first(f1)
    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  "firstSucc" should "complete the final future with the first one" in {
    val executor = getExecutor
    val util = getUtil
    val f0 = util.async(executor, () => 10)
    f0.sync
    val f1 = util.async(executor, () => { delay(); 11 })
    val f = f0.firstSucc(f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val executor = getExecutor
    val util = getUtil
    val f0 = util.async[Int](executor, () => throw new RuntimeException("test"))
    val f1 = util.async(executor, () => 11)
    val f = f0.firstSucc(f1)
    f.get should be(11)
  }

  it should "complete with the exception of the second future" in {
    val executor = getExecutor
    val util = getUtil
    val f0 = util.async[Int](executor, () => throw new RuntimeException("test 0"))
    f0.sync
    val f1 = util.async[Int](executor, () => { delay(); throw new RuntimeException("test 1") })
    val f = f0.firstSucc(f1)

    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  it should "complete with the exception of the first future" in {
    val executor = getExecutor
    val util = getUtil
    val f1 = util.async[Int](executor, () => throw new RuntimeException("test 1"))
    f1.sync
    val f0 = util.async[Int](executor, () => { delay(); throw new RuntimeException("test 0") })
    val f = f0.firstSucc(f1)

    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  def getExecutor: Executor
  def getUtil: Util
}