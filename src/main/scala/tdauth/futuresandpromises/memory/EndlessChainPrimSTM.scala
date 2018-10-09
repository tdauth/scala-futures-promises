package tdauth.futuresandpromises.memory

import java.util.concurrent.Executors

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.stm.PrimSTM

/**
 * Since it seems that the memory consumption by callbacks has been fixed and the benchmark from Scala FP does not work anymore,
 * we have to produce our own example based on the answer in [[https://users.scala-lang.org/t/how-does-promise-linking-work/3326/4]].
 *
 * This produces a `java.lang.OutOfMemoryError: Java heap space` when no promise linking is implemented.
 */
class EndlessChainPrimSTM(ex: Executor, arraySize: Int) extends PrimSTM[Unit](ex) {
  val array = new Array[Byte](arraySize)

  override def finalize = {
    // Reference the big array, so it is kept until the GC finalizes this instance.
    println("Done! " + array)
  }
}

object EndlessChainPrimSTM extends App {
  val arraySize = 1000000
  val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1

  {
    val ex = new JavaExecutor(Executors.newSingleThreadExecutor())
    val start = new EndlessChainPrimSTM(ex, arraySize)
    var previous = start
    var current: EndlessChainPrimSTM = null
    for (i <- 1 until tooManyArrays) {
      current = new EndlessChainPrimSTM(ex, arraySize)
      current.tryCompleteWith(previous)
      previous = current
    }

    // Never reached code if there is an OutOfMemoryError exception:
    start.trySuccess(())
    println("current: " + current.get)
  }

  println("Done!")
  // TODO #22 When it works, there is no garbage collecting?!
  // TODO #22 no exit of the program here?
}