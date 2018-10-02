package tdauth.futuresandpromises

/**
 * Similar to [[https://github.com/scala/scala/blob/2.12.x/test/files/run/t7336.scala t7336]].
 * Uses [[Future.thenWith]] which is basically transformWith in Scala FP.
 *
 *  This test uses recursive calls to [[Future.thenWith]] to create arrays whose
 *  combined size is slightly greater than the JVM heap size. A previous
 *  implementation of [[Future.thenWith]] would retain references to each array,
 *  resulting in a speedy OutOfMemoryError. Now, each array should be released soon
 *  after it is created and the test should complete without problems.
 */
abstract class AbstractRecursiveThenWithMemoryTest extends AbstractUnitSpec {
  getTestName should "not lead to memory exhaustion" in {
    def loop(i: Int, arraySize: Int): Future[Int] = {
      val array = new Array[Byte](arraySize)
      getSuccessfulFuture.thenWith { t =>
        val i = t.get()
        if (i == 0) {
          getSuccessfulFuture
        } else {
          array.size // Force closure to refer to array
          loop(i - 1, arraySize)
        }
      }
    }

    val arraySize = 1000000
    val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1
    val f = loop(tooManyArrays, arraySize)
    f.get
  }

  def getSuccessfulFuture: Future[Int]
}