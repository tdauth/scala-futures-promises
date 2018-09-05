package main.scala.futuresandpromises

trait Executor {
  def submit(f: () => Unit)
}