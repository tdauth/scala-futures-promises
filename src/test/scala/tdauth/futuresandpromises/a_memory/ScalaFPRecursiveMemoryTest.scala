package tdauth.futuresandpromises.a_memory

import java.util.concurrent.Executors

import tdauth.futuresandpromises.standardlibrary.{ScalaFPExecutor, ScalaFPFuture, ScalaFPPromise}

import scala.concurrent.ExecutionContext

class ScalaFPRecursiveMemoryTest extends AbstractRecursiveMemoryTest[ScalaFPFuture[Int]] {
  val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))

  override def getTestName: String = "ScalaFPRecursiveMemoryTest"

  def successfulFuture(): ScalaFPFuture[Int] = {
    val p = new ScalaFPPromise[Int](executor)
    p.trySuccess(0)
    p.future.asInstanceOf[ScalaFPFuture[Int]]
  }
  def flatMapOnSuccessfulFuture(i: Int, f: (Int) => ScalaFPFuture[Int]): ScalaFPFuture[Int] = {
    val p = new ScalaFPPromise[Int](executor)
    p.trySuccess(i)
    p.future.followedByWith(f).asInstanceOf[ScalaFPFuture[Int]]
  }

  def syncFuture(f: ScalaFPFuture[Int]) = f.get
}