package tdauth.futuresandpromises

import java.util.concurrent.ForkJoinPool

/**
 * Executor based on a Java executor.
 */
class JavaExecutor(val ex: java.util.concurrent.Executor) extends Executor {

  def this() = this(ForkJoinPool.commonPool())

  override def submit(f: () => Unit): Unit = {
    ex.execute(new Runnable() {
      override def run() {
        f.apply()
      }
    })
  }
}

object JavaExecutor {
  val global = new JavaExecutor
}