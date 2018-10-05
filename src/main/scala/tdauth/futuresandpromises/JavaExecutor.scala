package tdauth.futuresandpromises

import java.util.concurrent.ForkJoinPool

/**
 * Executor based on a Java executor.
 */
class JavaExecutor(val ex: java.util.concurrent.Executor) extends Executor {

  override def submit(f: () => Unit): Unit = {
    ex.execute(new Runnable() {
      override def run() {
        f.apply()
      }
    })
  }
}