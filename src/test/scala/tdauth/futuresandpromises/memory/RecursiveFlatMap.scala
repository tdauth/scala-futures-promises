package tdauth.futuresandpromises.memory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.comprehensive.ComprehensiveFuture
import tdauth.futuresandpromises.standardlibrary.ScalaFPFactory

/**
 * Copy&Paste from:
 * https://github.com/scala/scala/blob/2.12.x/test/files/run/t7336.scala
 *
 *  This test uses recursive calls to Future.flatMap to create arrays whose
 *  combined size is slightly greater than the JVM heap size. A previous
 *  implementation of Future.flatMap would retain references to each array,
 *  resulting in a speedy OutOfMemoryError. Now, each array should be freed soon
 *  after it is created and the test should complete without problems.
 */
object Test {
  val factory = new ScalaFPFactory

  def main(args: Array[String]) {
    def loop(i: Int, arraySize: Int): Future[Unit] = {
      val array = new Array[Byte](arraySize)
      ComprehensiveFuture.successful(factory, i).asInstanceOf[ComprehensiveFuture[Int]].flatMap { i =>
        if (i == 0) {
          ComprehensiveFuture.successful(factory, ())
        } else {
          array.size // Force closure to refer to array
          loop(i - 1, arraySize)
        }

      }
    }

    val arraySize = 1000000
    val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1
    Await.ready(loop(tooManyArrays, arraySize).asInstanceOf[ComprehensiveFuture[Unit]], Duration.Inf)
  }
}