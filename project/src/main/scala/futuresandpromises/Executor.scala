package main.scala.futuresandpromises

trait Executor {
  def submit[Func](f: Func)
}