package main.scala.futuresandpromises

trait Promise[T] {
  // TODO define basic constructor
  //abstract def this()
  def future(): Future[T]
  def tryComplete(v: Try[T]): Boolean

  // These two methods are required since Try is defined as trait:
  def createTryFromValue(v: T): Try[T]
  def createTryFromException(e: Exception): Try[T]

  // derived methods:
  def trySuccess(v: T): Boolean = {
    this.tryComplete(createTryFromValue(v))
  }

  def tryFailure(e: Exception): Boolean = {
    this.tryComplete(createTryFromException(e))
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