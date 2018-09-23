package tdauth.futuresandpromises.nonderived

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor

class NonDerivedExecutor(executionContext: ExecutionContext) extends ScalaFPExecutor(executionContext) {

  def this() = this(ExecutionContext.global)
}

object NonDerivedExecutor {
  val global = new NonDerivedExecutor
}