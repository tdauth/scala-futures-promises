package tdauth.futuresandpromises.memory

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration.Duration

/**
 * Since it seems that the memory consumption by callbacks has been fixed and the benchmark from Scala FP does not work anymore,
 * we have to produce our own example based on the answer in [[https://users.scala-lang.org/t/how-does-promise-linking-work/3326/4]].
 *
 * This produces a `java.lang.OutOfMemoryError: Java heap space` when no promise linking is implemented.
 */
// TODO #22 Actually this should inherit the DefaultPromise! Otherwise, it might get released since we only need its field p!
class EndlessChainPrimScalaFP(arraySize: Int) {
  val p = Promise.apply[Unit]
  val array = new Array[Byte](arraySize)

  override def finalize = {
    // Reference the big array.
    println("Done! " + array)
  }
}

object EndlessChainPrimScalaFP extends App {
  val arraySize = 1000000
  val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1

  {
    val start = new EndlessChainPrimScalaFP(arraySize)
    var previous = start
    var current: EndlessChainPrimScalaFP = null
    for (i <- 1 until tooManyArrays) {
      current = new EndlessChainPrimScalaFP(arraySize)
      current.p.completeWith(previous.p.future)
      previous = current
    }

    // Never reached code if there is an OutOfMemoryError exception:
    start.p.trySuccess(())
    println("current: " + Await.result(current.p.future, Duration.Inf))
  }

  println("Done!")
}