package test.scala.futuresandpromises.standardlibrary

import org.scalatest._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import main.scala.futuresandpromises.standardlibrary.ScalaFPExecutor
import main.scala.futuresandpromises.standardlibrary.ScalaFPUtil
import main.scala.futuresandpromises.PredicateNotFulfilled

class FutureTest extends FlatSpec with Matchers {
  "A future" should "be created asynchronously" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => { Thread.sleep(1000); 10 })
    future.isReady should be(false)
    future.get should be(10)
    future.isReady should be(true)
  }

  "The callback" should "be called asynchronously" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10).then(t => t.get() + 10)
    future.get should be(20)
  }

  "The guard" should "be throw the exception PredicateNotFulfilled" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10).guard(v => v != 10)
    the[PredicateNotFulfilled] thrownBy future.get should have message null
  }

  "The guard" should "not throw any exception" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async(executor, () => 10).guard(v => v == 10)
    future.get should be(10)
  }

  "The guard" should "throw the initial exception" in {
    val executor = new ScalaFPExecutor
    val future = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test")).guard(v => v == 10)
    the[RuntimeException] thrownBy future.get should have message "test"
  }

  "The first future" should "be chosen over the second one" in {
    val executor = new ScalaFPExecutor
    val f0 = ScalaFPUtil.async(executor, () => 10)
    val f1 = ScalaFPUtil.async(executor, () => 11)
    val f = f0.orElse(f1)
    f.get should be(10)
  }

  "The second future" should "be chosen over the first one" in {
    val executor = new ScalaFPExecutor
    val f0 = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test"))
    val f1 = ScalaFPUtil.async(executor, () => 11)
    val f = f0.orElse(f1)
    f.get should be(11)
  }

  "The first failing future" should "be chosen over the second one" in {
    val executor = new ScalaFPExecutor
    val f0 = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test 0"))
    val f1 = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test 1"))
    val f = f0.orElse(f1)
    the[RuntimeException] thrownBy f.get should have message "test 0"
  }

  "The first future" should "complete the final future before second one" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val f0 = ScalaFPUtil.async(executor, () => 10)
    val f1 = ScalaFPUtil.async(executor, () => 11)
    val f = f0.first(f1)
    f.get should be(10)
  }

  "The second future" should "complete the final future before the first one" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val f1 = ScalaFPUtil.async(executor, () => 11)
    val f0 = ScalaFPUtil.async(executor, () => 10)
    val f = f0.first(f1)
    f.get should be(11)
  }

  "The first future" should "complete the final future successfully before second one" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val f0 = ScalaFPUtil.async(executor, () => 10)
    val f1 = ScalaFPUtil.async(executor, () => 11)
    val f = f0.firstSucc(f1)
    f.get should be(10)
  }

  "The second future" should "complete the final future successfully before the first one" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val f0 = ScalaFPUtil.async[Int](executor, () => throw new RuntimeException("test"))
    val f1 = ScalaFPUtil.async(executor, () => 11)
    val f = f0.firstSucc(f1)
    f.get should be(11)
  }
}
