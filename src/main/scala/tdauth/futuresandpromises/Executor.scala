package tdauth.futuresandpromises

trait Executor {
  def submit(f: () => Unit): Unit
  def shutdown: Unit = { }
}