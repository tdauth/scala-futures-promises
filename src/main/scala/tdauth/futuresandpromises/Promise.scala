package tdauth.futuresandpromises

trait Promise[T] {
  // Basic methods:
  def future(): Future[T]
  def tryComplete(v: Try[T]): Boolean

  // Derived methods:
  def trySuccess(v: T): Boolean = this.tryComplete(new Try(v))

  def tryFailure(e: Throwable): Boolean = this.tryComplete(new Try(e))

  def tryCompleteWith(f: Future[T]): Unit = f.onComplete(this.tryComplete(_))

  def trySuccessWith(f: Future[T]): Unit = f.onSuccess(this.trySuccess(_))

  def tryFailureWith(f: Future[T]): Unit = f.onFailure(this.tryFailure(_))
}