package tdauth.futuresandpromises.cas

import scala.concurrent.ExecutionContext

import java.util.concurrent.ForkJoinPool

import tdauth.futuresandpromises.Executor

class CasExecutor(val ex : java.util.concurrent.Executor) extends Executor {

  def this() = this(ForkJoinPool.commonPool())

  override def submit(f: () => Unit): Unit = {
    ex.execute(new Runnable() {
      override def run() {
        f.apply()
      }
    })
  }
}

object CasExecutor {
  val global = new CasExecutor
}