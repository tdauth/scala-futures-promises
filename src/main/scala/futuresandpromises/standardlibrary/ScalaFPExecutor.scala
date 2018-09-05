package main.scala.futuresandpromises.standardlibrary

import main.scala.futuresandpromises.Executor
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class ScalaFPExecutor extends Executor {
  val executionContext = ExecutionContext.global

  override def submit(f: () => Unit) = {
    executionContext.execute(new Runnable() {
      def run() {
        f()
      }
    })
  }
}