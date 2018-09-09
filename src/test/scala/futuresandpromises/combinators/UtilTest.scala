package tdauth.futuresandpromises.combinators

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.UnitSpec
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor

class UtilTest extends UnitSpec {
  "firstN" should "should throw an exception" in {
    val result = CombinatorsUtil.firstN(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three futures" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val futures = Vector.tabulate(10)(n => {
      CombinatorsUtil.async(executor, () => {
        Thread.sleep(n * 100)

        if (n % 2 == 0) {
          n
        } else {
          throw new RuntimeException("test")
        }
      })
    })

    val result = CombinatorsUtil.firstN(futures, 3).get

    result.length should be(3)

    val t0 = result(0)
    t0._1 should be(0)
    t0._2.get should be(0)

    val t1 = result(1)
    t1._1 should be(1)
    the[Exception] thrownBy t1._2.get should have message "test"

    val t3 = result(2)
    t3._1 should be(2)
    t3._2.get should be(2)
  }

  "firstNSucc" should "throw an exception" in {
    val result = CombinatorsUtil.firstNSucc(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three successful futures" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val futures = Vector.tabulate(10)(n => {
      CombinatorsUtil.async(executor, () => {
        Thread.sleep(n * 100)

        if (n % 2 == 0) {
          n
        } else {
          throw new RuntimeException("test")
        }
      })
    })

    val result = CombinatorsUtil.firstNSucc(futures, 3).get

    result.length should be(3)

    val t0 = result(0)
    t0._1 should be(0)
    t0._2 should be(0)

    val t1 = result(1)
    t1._1 should be(2)
    t1._2 should be(2)

    val t3 = result(2)
    t3._1 should be(4)
    t3._2 should be(4)
  }

  it should "fail with one of the futures" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val futures = Vector.tabulate(10)(n => {
      CombinatorsUtil.async[Int](executor, () => {
        throw new RuntimeException("test " + n)
      })
    })

    val result = CombinatorsUtil.firstNSucc(futures, 3)

    the[RuntimeException] thrownBy result.get should have message "test 7"
  }
}