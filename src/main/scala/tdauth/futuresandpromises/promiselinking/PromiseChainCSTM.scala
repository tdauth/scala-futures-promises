package tdauth.futuresandpromises.promiselinking

import java.util.concurrent.Executors

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.core.stm.CSTM
/**
 * Since it seems that the memory consumption by callbacks has been fixed and the benchmark from Scala FP does
 * not work anymore (see [[ScalaFPRecursiveMemoryTest]]), we have to produce our own example based on the answer in
 * [[https://users.scala-lang.org/t/how-does-promise-linking-work/3326 How does promise linking work?]].
 *
 * This produces a `java.lang.OutOfMemoryError: Java heap space` when no promise linking is implemented.
 */
class PromiseChainCSTM(ex: Executor, arraySize: Int) extends CSTM[Unit](ex) {
  val array = new Array[Byte](arraySize)

  override def finalize: Unit = {
    // Reference the big array, so it is kept until the GC finalizes this instance.
    println("Done! " + array)
  }
}

object PromiseChainCSTM extends App {
  val arraySize = 1000000
  val tooManyArrays = (Runtime.getRuntime().totalMemory() / arraySize).toInt + 1

  {
    val ex = new JavaExecutor(Executors.newSingleThreadExecutor())
    val start = new PromiseChainCSTM(ex, arraySize)
    var previous = start
    var current: PromiseChainCSTM = null
    for (i <- 1 until tooManyArrays) {
      current = new PromiseChainCSTM(ex, arraySize)
      current.tryCompleteWith(previous)
      previous = current
    }

    // Never reached code if there is an OutOfMemoryError exception:
    start.trySuccess(())
    println("current: " + current.getP)
  }

  println("Done!")
  // TODO #22 When it works, there is no garbage collecting?!
  // TODO #22 no exit of the program here?
}