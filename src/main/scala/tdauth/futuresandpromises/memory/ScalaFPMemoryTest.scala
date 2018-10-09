package tdauth.futuresandpromises.memory

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPPromise

object ScalaFPRecursiveMemoryTest extends App {
  val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))

  def loop(i: Int, arraySize: Int): Future[Int] = {
    val array = new Array[Byte](arraySize)
    val p = new ScalaFPPromise[Int]
    p.trySuccess(i)
    p.future.thenWith(t => {
      val i = t.get
      if (i == 0) {
        val p = new ScalaFPPromise[Int]
        p.trySuccess(i)
        p.future
      } else {
        array.size // Force closure to refer to array
        loop(i - 1, arraySize)
      }
    })
  }

  val arraySize = 1000000
  val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1
  val f = loop(tooManyArrays, arraySize)
  f.get
}