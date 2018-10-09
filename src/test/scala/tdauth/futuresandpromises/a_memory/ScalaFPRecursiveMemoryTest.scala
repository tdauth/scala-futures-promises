package tdauth.futuresandpromises.a_memory

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPFuture
import tdauth.futuresandpromises.standardlibrary.ScalaFPPromise

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