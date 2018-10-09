package tdauth.futuresandpromises.a_memory

import tdauth.futuresandpromises.AbstractUnitSpec

/**
 * Based on [[https://github.com/scala/scala/blob/2.12.x/test/files/run/t7336.scala t7336]]
 */
abstract class AbstractRecursiveMemoryTest[F] extends AbstractUnitSpec {

  getTestName should "run successfully and not exceed the memory limits" in {
    runTest
  }

  def runTest() = {
    def loop(i: Int, arraySize: Int): F = {
      val array = new Array[Byte](arraySize)
      flatMapOnSuccessfulFuture(i, { i =>
        if (i == 0) {
          successfulFuture
        } else {
          array.size // Force closure to refer to array
          loop(i - 1, arraySize)
        }
      })
    }

    val arraySize = 1000000
    val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1
    val f = loop(tooManyArrays, arraySize)
    syncFuture(f)
  }

  def successfulFuture(): F
  def flatMapOnSuccessfulFuture(i : Int, f: (Int) => F): F
  def syncFuture(f: F)
}