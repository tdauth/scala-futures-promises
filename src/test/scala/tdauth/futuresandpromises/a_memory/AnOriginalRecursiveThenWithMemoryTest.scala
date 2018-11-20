package tdauth.futuresandpromises.a_memory

import tdauth.futuresandpromises.AbstractUnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
 * Start the name with A to run this test as the first one.
 *
 * This test uses recursive calls to Future.flatMap to create arrays whose
 *  combined size is slightly greater than the JVM heap size. A previous
 *  implementation of Future.flatMap would retain references to each array,
 *  resulting in a speedy OutOfMemoryError. Now, each array should be freed soon
 *  after it is created and the test should complete without problems.
 */
class AnOriginalRecursiveThenWithMemoryTest extends AbstractUnitSpec {
  getTestName should "call a function asynchronously" in {
    def loop(i: Int, arraySize: Int): Future[Unit] = {
      val array = new Array[Byte](arraySize)
      Future.successful(i).flatMap { i =>
        if (i == 0) {
          Future.successful(())
        } else {
          array.size // Force closure to refer to array
          loop(i - 1, arraySize)
        }

      }
    }

    val arraySize = 1000000
    val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1
    Await.ready(loop(tooManyArrays, arraySize), Duration.Inf)
  }

  override def getTestName: String = "OriginalRecursiveThenWithMemoryTest"
}