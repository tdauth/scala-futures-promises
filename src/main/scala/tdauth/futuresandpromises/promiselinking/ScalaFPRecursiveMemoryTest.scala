package tdauth.futuresandpromises.promiselinking

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPPromise

/**
 * Based on [[https://github.com/scala/scala/blob/2.12.x/test/files/run/t7336.scala t7336]] but with our own implementation which does NOT
 * use promise linking.
 * Since this test does not lead to an OutOfMemoryException, it seems to be true that it has been a compiler bug in Scala 2.11,
 * not to release the closures, nothing more.
 * This information comes from the user "tarsa": [[https://users.scala-lang.org/t/how-does-promise-linking-work/3326 How does promise linking work?]].
 * TODO Test it with Scala 2.11 -> it should lead to an OutOfMemoryException here!
 */
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