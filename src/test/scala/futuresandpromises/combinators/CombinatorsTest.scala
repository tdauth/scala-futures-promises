package tdauth.futuresandpromises.combinators

import tdauth.futuresandpromises.UnitSpec
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPUtil

class CombinatorsTest extends UnitSpec {
  "firstSuccWithOrElse" should "complete the final future with the first one" in {
    val executor = new ScalaFPExecutor
    val f0 = ScalaFPUtil.async(executor, () => 10)
    val f1 = ScalaFPUtil.async(executor, () => { Thread.sleep(1000); 11 })
    val f = Combinators.firstSuccWithOrElse(f0, f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one" in {
    val executor = new ScalaFPExecutor
    val f0 = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test"))
    val f1 = ScalaFPUtil.async(executor, () => 11)
    val f = Combinators.firstSuccWithOrElse(f0, f1)
    f.get should be(11)
  }

  it should "complete with the exception of the second future" in {
    val executor = new ScalaFPExecutor
    val f0 = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test 0"))
    val f1 = ScalaFPUtil.async[Int](executor, () => { Thread.sleep(1000); throw new RuntimeException("test 1") })
    val f = Combinators.firstSuccWithOrElse(f0, f1)

    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  it should "complete with the exception of the first future" in {
    val executor = new ScalaFPExecutor
    val f0 = ScalaFPUtil.async[Int](executor, () => { Thread.sleep(1000); throw new RuntimeException("test 0") })
    val f1 = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test 1"))
    val f = Combinators.firstSuccWithOrElse(f0, f1)

    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  "firstNWithFirst" should "should throw an exception" in {
    val result = Combinators.firstNWithFirst(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three futures" in {
    val executor = new ScalaFPExecutor
    val futures = Vector.tabulate(3)(n => {
      ScalaFPUtil.async(executor, () => {
        if (n % 2 == 0) {
          n
        } else {
          throw new RuntimeException("test")
        }
      })
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