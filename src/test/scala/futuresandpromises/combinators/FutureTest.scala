package tdauth.futuresandpromises.combinators

import tdauth.futuresandpromises.UnitSpec
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor

class FutureTest extends UnitSpec {

  "A combinator future" should "complete the final future with the first one with the help of firstSuccWithOrElse" in {
    val executor = new ScalaFPExecutor
    val f0 = CombinatorsUtil.async(executor, () => 10)
    val f1 = CombinatorsUtil.async(executor, () => { Thread.sleep(1000); 11 })
    val f = f0.asInstanceOf[CombinatorsFuture[Int]].firstSuccWithOrElse(f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one with the help of firstSuccWithOrElse" in {
    val executor = new ScalaFPExecutor
    val f0 = CombinatorsUtil.async[Int](executor, () => throw new RuntimeException("test"))
    val f1 = CombinatorsUtil.async(executor, () => 11)
    val f = f0.asInstanceOf[CombinatorsFuture[Int]].firstSuccWithOrElse(f1)
    f.get should be(11)
  }

  it should "complete with the exception of the second future with the help of firstSuccWithOrElse" in {
    val executor = new ScalaFPExecutor
    val f0 = CombinatorsUtil.async[Int](executor, () => throw new RuntimeException("test 0"))
    val f1 = CombinatorsUtil.async[Int](executor, () => { Thread.sleep(1000); throw new RuntimeException("test 1") })
    val f = f0.asInstanceOf[CombinatorsFuture[Int]].firstSuccWithOrElse(f1)

    the[RuntimeException] thrownBy f.get should have message "test 1"
  }

  it should "complete with the exception of the first future with the help of firstSuccWithOrElse" in {
    val executor = new ScalaFPExecutor
    val f0 = CombinatorsUtil.async[Int](executor, () => { Thread.sleep(1000); throw new RuntimeException("test 0") })
    val f1 = CombinatorsUtil.async[Int](executor, () => throw new RuntimeException("test 1"))
    val f = f0.asInstanceOf[CombinatorsFuture[Int]].firstSuccWithOrElse(f1)

    the[RuntimeException] thrownBy f.get should have message "test 0"
  }
}
