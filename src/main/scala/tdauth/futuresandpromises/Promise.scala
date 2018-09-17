package tdauth.futuresandpromises

trait Promise[T] {
  // Basic methods:
  def future(): Future[T]
  def tryComplete(v: Try[T]): Boolean

  def factory: Factory

  // Derived methods:
  def trySuccess(v: T): Boolean = {
    this.tryComplete(factory.createTryFromValue(v))
  }

  def tryFailure(e: Throwable): Boolean = {
    this.tryComplete(factory.createTryFromException(e))
  }

  def tryCompleteWith(f: Future[T]): Unit = {
    val p = this
    f.onComplete((t: Try[T]) => p.tryComplete(t))
  }

  def trySuccessWith(f: Future[T]): Unit = {
    val p = this
    f.onComplete((t: Try[T]) => if (t.hasValue) { p.tryComplete(t); })
  }

  def tryFailureWith(f: Future[T]): Unit = {
    val p = this
    f.onComplete((t: Try[T]) => if (t.hasException) { p.tryComplete(t); })
  }
}