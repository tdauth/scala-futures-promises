package tdauth.futuresandpromises.combinators

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.UnitSpec
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor

class FutureTest extends UnitSpec {

  "A combinator future" should "complete the final future with the first one with the help of firstSuccWithOrElse" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val f0 = CombinatorsUtil.async(executor, () => 10)
    val f1 = CombinatorsUtil.async(executor, () => 11)
    val f = f0.asInstanceOf[CombinatorsFuture[Int]].firstSuccWithOrElse(f1)
    f.get should be(10)
  }

  it should "complete the final future with the second one with the help of firstSuccWithOrElse" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val f0 = CombinatorsUtil.async[Int](executor, () => throw new RuntimeException("test"))
    val f1 = CombinatorsUtil.async(executor, () => 11)
    val f = f0.asInstanceOf[CombinatorsFuture[Int]].firstSuccWithOrElse(f1)
    f.get should be(11)
  }

  /*
  TODO check for timeout
  it should "timeout with the help of firstSuccWithOrElse since both futures fail" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val f0 = CombinatorsUtil.async[Int](executor, () => throw new RuntimeException("test"))
    val f1 = CombinatorsUtil.async[Int](executor, () => throw new RuntimeException("test"))
    val f = f0.asInstanceOf[CombinatorsFuture[Int]].firstSuccWithOrElse(f1)
    f.get should be(11)
  }
  */
}
