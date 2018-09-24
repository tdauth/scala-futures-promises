package tdauth.futuresandpromises.standardlibrary

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.Executor

class ScalaFPExecutor(val executionContext: ExecutionContext) extends Executor {

  def this() = this(ExecutionContext.global)

  override def submit(f: () => Unit): Unit = {
    executionContext.execute(new Runnable() {
      def run() {
        f()
      }
    })
  }
}

object ScalaFPExecutor {
  val global = new ScalaFPExecutor
}