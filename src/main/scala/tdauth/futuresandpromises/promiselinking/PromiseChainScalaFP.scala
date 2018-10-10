package tdauth.futuresandpromises.promiselinking

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration.Duration

// TODO #22 Actually this should inherit the DefaultPromise! Otherwise, it might get released since we only need its field p!
class PromiseChainScalaFP(arraySize: Int) {
  val p = Promise.apply[Unit]
  val array = new Array[Byte](arraySize)

  override def finalize = {
    // Reference the big array.
    println("Done! " + array)
  }
}

object PromiseChainScalaFP extends App {
  val arraySize = 1000000
  val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1

  {
    val start = new PromiseChainScalaFP(arraySize)
    var previous = start
    var current: PromiseChainScalaFP = null
    for (i <- 1 until tooManyArrays) {
      current = new PromiseChainScalaFP(arraySize)
      current.p.completeWith(previous.p.future)
      previous = current
    }

    // Never reached code if there is an OutOfMemoryError exception:
    start.p.trySuccess(())
    println("current: " + Await.result(current.p.future, Duration.Inf))
  }

  println("Done!")
}