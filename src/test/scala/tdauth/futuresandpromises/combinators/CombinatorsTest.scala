package tdauth.futuresandpromises.combinators

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractUnitSpec
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPPromise

class CombinatorsTest extends AbstractUnitSpec {
  override def getTestName: String = "Combinators"
  def getPromise: Promise[Int] = new ScalaFPPromise[Int](executor)

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))

  "firstSuccWithOrElse" should "complete the final future with the first one" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.trySuccess(10)
    val p1 = getPromise
    val f1 = p1.future()
    val f = Combinators.firstSuccWithOrElse(f0, f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.trySuccess(11)
    val f = Combinators.firstSuccWithOrElse(f0, f1)
    f.get should be(11)
  }

  it should "complete with the exception of the first future" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test 0"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.tryFailure(new RuntimeException("test 1"))
    // When onComplete is registered to f0, it tries to fail the promise and therefore f will fail with f0's failure.
    val f = Combinators.firstSuccWithOrElse(f0, f1)

    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  it should "complete with the exception of the second future" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test 0"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.tryFailure(new RuntimeException("test 1"))
    // When onComplete is registered to f1, it tries to fail the promise and therefore f will fail with f1's failure.
    val f = Combinators.firstSuccWithOrElse(f1, f0)

    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  "firstWithFirstN" should "complete the final future with the first one" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.trySuccess(10)
    val p1 = getPromise
    val f1 = p1.future()
    val f = Combinators.firstWithFirstN(f0, f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val p1 = getPromise
    val f1 = p1.future()
    p1.trySuccess(11)
    val p0 = getPromise
    val f0 = p0.future()
    val f = Combinators.firstWithFirstN(f0, f1)
    f.get should be(11)
  }

  it should "complete the final future with the second one although it fails" in {
    val p1 = getPromise
    val f1 = p1.future()
    p1.tryFailure(new RuntimeException("test 1"))
    val p0 = getPromise
    val f0 = p0.future()
    val f = Combinators.firstWithFirstN(f0, f1)
    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  "firstSuccWithFirstNSucc" should "complete the final future with the first one" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.trySuccess(10)
    val p1 = getPromise
    val f1 = p1.future()
    p1.trySuccess(11)
    val f = Combinators.firstSuccWithFirstNSucc(f0, f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.trySuccess(11)
    val f = Combinators.firstSuccWithFirstNSucc(f0, f1)
    f.get should be(11)
  }

  it should "complete with the exception of the second future" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test 0"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.tryFailure(new RuntimeException("test 1"))
    val f = Combinators.firstSuccWithFirstNSucc(f0, f1)
    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  it should "complete with the exception of the first future" in {
    val p1 = getPromise
    val f1 = p1.future()
    p1.tryFailure(new RuntimeException("test 1"))
    val p0 = getPromise
    val f0 = p0.future()
    val f = Combinators.firstSuccWithFirstNSucc(f0, f1)
    p0.tryFailure(new RuntimeException("test 0"))
    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  "orElseWithFirstNSucc" should "complete the final future with first one over the second one" in {
    val p0 = getPromise
    val f0 = p0.future
    val p1 = getPromise
    val f1 = p1.future()
    val f = Combinators.orElseWithFirstNSucc(f0, f1)
    p0.trySuccess(10)
    p1.trySuccess(11)
    f.get should be(10)
  }

  it should "complete the final future with the second one over the first one" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.trySuccess(11)
    val f = Combinators.orElseWithFirstNSucc(f0, f1)
    f.get should be(11)
  }

  it should "complete the final future with the first one over the second one when both are failing" in {
    val p0 = getPromise
    val f0 = p0.future()
    p0.tryFailure(new RuntimeException("test 0"))
    val p1 = getPromise
    val f1 = p1.future()
    p1.tryFailure(new RuntimeException("test 1"))
    val f = Combinators.orElseWithFirstNSucc(f0, f1)
    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  "firstNWithFirst" should "throw an exception" in {
    val result = Combinators.firstNWithFirst(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three futures" in {
    val futures = Vector.tabulate(3)(n => {
      val p = getPromise
      if (n % 2 == 0) {
        p.trySuccess(n)
      } else {
        p.tryFailure(new RuntimeException("test"))
      }

      p.future()
    })

    val result = Combinators.firstNWithFirst(futures, 3).get

    result.length should be(3)

    var counter = 0

    for (t <- result) {
      t._1 match {
        case 0 => {
          t._2.get should be(0)
          counter += 1
        }
        case 1 => {
          the[Exception] thrownBy t._2.get should have message "test"
          counter += 1
        }
        case 2 => {
          t._2.get should be(2)
          counter += 1
        }
        case _ => fail("Unexpected result: " + t)
      }
    }

    counter should be(3)
  }
}