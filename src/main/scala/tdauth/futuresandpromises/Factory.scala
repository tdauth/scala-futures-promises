package tdauth.futuresandpromises

/**
 * The factory allows concrete implementation of the traits to create instances of the concrete types.
 * These are required for the derived methods in the traits.
 */
trait Factory {
  def createPromise[T]: Promise[T]
  def createTryFromValue[T](v: T): Try[T]
  def createTryFromException[T](e: Throwable): Try[T]
  /**
   * This method is required by {@link Util#async} to assign the executor which is used to complete the future initially.
   * The executor should be stored to be used by the future's callback.
   */
  def assignExecutorToFuture[T](f: Future[T], e: Executor) : Unit
}