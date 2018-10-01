package tdauth.futuresandpromises.standardlibrary

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.Executor

class ScalaFPExecutor(val executionContext: ExecutionContext) extends Executor {

  def this() = this(ExecutionContext.global)

  override def submit(f: () => Unit): Unit = {
    executionContext.execute(new Runnable() {
      override def run() {
        f.apply()
      }
    })
  }
}

object ScalaFPExecutor {
  val global = new ScalaFPExecutor
}