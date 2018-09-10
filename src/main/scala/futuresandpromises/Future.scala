package tdauth.futuresandpromises

import java.util.concurrent.atomic.AtomicBoolean

import scala.util.control.NonFatal

/**
 * Stores the result value of an asynchronous computation. Can either be empty, store a successful result value or an exception if it has failed.
 *
 * Futures are first-class values which can be combined to chains of asynchronous computations which are non-blocking.
 * They can be completed by {@link Promise}.
 *
 * @todo In the C++ paper, our future has single-read and single-callback semantics. This trait does not restrict to these semantics.
 */
trait Future[T] {
  // Basic methods:
  def get: T
  def isReady: Boolean
  def then[S](f: (Try[T]) => S): Future[S]

  def factory: Factory

  // Derived methods:
  def onComplete(f: (Try[T]) => Unit): Unit = {
    this.then(f)
  }

  /**
   * Allows to filter a future matching a user-defined condition.
   *
   * Registers the callback predicate f to the future which gets the successful result value of the future and returns a boolean value.
   * If the callback returns true, the future is completed with its successful result value.
   * If the callback returns false, the future is completed with an exception of the type {@link PredicateNotFulfilled}.
   * If the future has failed when the callback predicate should be called, it fails with its original exception.
   *
   * @param f The registered callback predicate.
   * @return Returns the filtered future.
   */
  def guard(f: (T) => Boolean): Future[T] = {
    return this.then[T]((t: Try[T]) => {
      val v: T = t.get() // rethrows the exception if necessary

      if (!f(v)) {
        throw new PredicateNotFulfilled
      }

      v
    }: T)
  }

  /**
   * Returns either the this future, or if it has failed the passed future.
   * If both futures fail, it fails with the exception of this future.
   *
   * @param other Another future which is chosen if this future fails.
   * @return Returns the selection between both futures.
   */
  def orElse(other: Future[T]): Future[T] = {
    return this.then[T]((t: Try[T]) => {
      if (t.hasException) {
        try {
          other.get
        } catch {
          case NonFatal(x) => t.get // will rethrow if failed
        }
      } else {
        t.get
      }
    })
  }

  def first(other: Future[T]): Future[T] = {
    val p = factory.createPromise[T]

    this.onComplete((t: Try[T]) => {
      p.tryComplete(t)
    })

    other.onComplete((t: Try[T]) => {
      p.tryComplete(t)
    })

    p.future
  }

  /**
   * Completes the returned future with the first successfully completed future of the two passed futures this and other.
   * If both futures fail, it fails with the final failing future to prevent any starvation of the program.
   * In the C++ implementation we did rely on the assumption that the promise will be deleted and the resulting future would fail with {@link BrokenPromise}.
   * However, we cannot make such an assumption in this implementation. since Scala has a garbage collection.
   *
   * @see {@link combinators.Combinators#firstSuccWithOrElse} for a different implementation based on {@link #orElse}.
   */
  def firstSucc(other: Future[T]): Future[T] = {
    val p = factory.createPromise[T]
    /*
     * This context is required to store if both futures have failed to prevent starvation.
     */
    val ctx = (new AtomicBoolean(false), new AtomicBoolean(false))

    this.onComplete((t: Try[T]) => {
      if (t.hasException) {
        ctx._1.set(true)

        if (ctx._2.get) {
          p.tryComplete(t)
        }
      } else {
        p.trySuccess(t.get())
      }
    })

    other.onComplete((t: Try[T]) => {
      if (t.hasException) {
        ctx._2.set(true)

        if (ctx._1.get) {
          p.tryComplete(t)
        }
      } else {
        p.trySuccess(t.get())
      }
    })

    p.future
  }
}