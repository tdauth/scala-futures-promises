package tdauth.futuresandpromises

/**
 * The factory allows concrete implementation of the traits to create instances of the concrete types.
 * These are required for the derived methods in the traits.
 */
trait Factory {
  /**
   * Creates a new empty promise.
   * All futures created from the promise will inherit the executor ex.
   */
  def createPromise[T](ex : Executor): Promise[T]
  /**
   * This method is only required to provide {@link tdauth.futuresandpromises.comprehensive.ComprehensiveFuture#unit}.
   */
  def createTry(): Try[Unit]
  def createTryFromValue[T](v: T): Try[T]
  def createTryFromException[T](e: Throwable): Try[T]
}