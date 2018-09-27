package tdauth.futuresandpromises.comprehensive

import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

/**
 * This trait provides all of our Advanced Futures and Promises functionality + all methods provided by Scala FP's Promise trait.
 */
trait ComprehensivePromise[T] extends Promise[T] {
  // future and tryComplete are already defined as abstract basic methods in Promise.

  /**
   * This is abstract in Scala FP although it could be implemented this way.
   * It is probably abstract since it is abstract for Future, too and DefaultPromise implements both types.
   */
  def isCompleted: Boolean = future.isReady

  /**
   * Scala FP implements this the same way. Copy & paste.
   */
  def complete(result: Try[T]): this.type = if (tryComplete(result)) this else throw new IllegalStateException("Promise already completed.")

  /**
   * This method is basically {@link #tryCompleteWith} but returns this promise and makes an additional check.
   * Scala FP implements this the same way. Copy & paste.
   */
  def completeWith(other: Future[T]): this.type = {
    if (other ne this.future) { // this tryCompleteWith this doesn't make much sense
      other.onComplete(this tryComplete _)
    }
    this
  }

  /**
   * Scala FP implements this the same way. Copy & paste.
   */
  def failure(cause: Throwable): this.type = complete(factory.createTryFromException(cause))

  /**
   * Scala FP implements this the same way. Copy & paste.
   */
  def success(value: T): this.type = complete(factory.createTryFromValue(value))

  // tryCompleteWith is deprecated because it is the same as completeWith, so don't implement it.

  // tryFailure and trySuccess are already defined as derived methods in Promise.
}

/**
 * Our implementation requires a factory.
 * Hence, it has to be passed as parameter.
 * The factory could be used directly instead.
 * The object would only make sense in a concrete implementation such as the one based on Scala FP where the factory would not be necessary.
 */
object ComprehensivePromise {

  final def apply[T](f: Factory): Promise[T] = f.createPromise[T]

  final def failed[T](f: Factory, exception: Throwable): Promise[T] = fromTry(f, f.createTryFromException(exception))

  final def successful[T](f: Factory, result: T): Promise[T] = fromTry(f, f.createTryFromValue(result))

  final def fromTry[T](f: Factory, result: Try[T]): Promise[T] = {
    val p = apply[T](f)
    p.tryComplete(result)
    p
  }
}