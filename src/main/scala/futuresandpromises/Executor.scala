package tdauth.futuresandpromises

trait Executor {
  def submit(f: () => Unit)
}