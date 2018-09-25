package tdauth.futuresandpromises

import java.util.concurrent.atomic.AtomicInteger

import scala.reflect.ClassTag
import scala.util.Failure
import scala.util.Success
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal

/**
 * Stores the result value of an asynchronous computation. Can either be empty, store a successful result value or an exception if it has failed.
 *
 * Futures are first-class values which can be combined to chains of asynchronous computations which are non-blocking.
 * They can be completed by {@link Promise}.
 * A future in Scala has multiple-read semantics and can have multiple callbacks.
 *
 * As Future is defined with a covariant T in Scala FP, we do the same here.
 */
trait Future[+T] {
  // Basic methods:

  /**
   * Blocks until the future is completed and returns its result value.
   * If it is completed by an exception, the exception is rethrown by the call.
   *
   * @return Returns the result value of the future.
   */
  def get: T

  /**
   * Checks if the future has been completed or is still uncompleted.
   * @return Returns true if the future has been completed. Otherwise, it returns false.
   */
  def isReady: Boolean

  /**
   * Registers a callback function which is submitted to the future's executor when the future is completed.
   * The callback function gets the result of the future as parameter and its return value is used for the result of a new future.
   * If the callback function call throws an exception, the new resulting future is completed with the exception rather than the return value of the callback function.
   *
   * @param f Callback function which is executed by the same executor as this future and which gets the result of this future and of which the return value or thrown exception completes the newly created future.
   * @return Returns a newly created future which is completed by the callback function at some point in time.
   */
  def then[S](f: (Try[T]) => S): Future[S]

  def factory: Factory

  // Derived methods:
  def onComplete(f: (Try[T]) => Unit): Unit = this.then(f)

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

      if (!f.apply(v)) {
        throw new PredicateNotFulfilled
      }

      v
    })
  }

  /**
   * Returns either the this future, or if it has failed the passed future.
   * If both futures fail, it fails with the exception of this future.
   *
   * @param other Another future which is chosen if this future fails.
   * @return Returns the selection between both futures.
   */
  def orElse(other: Future[T]): Future[T] = {
    return this.then((t: Try[T]) => {
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
    p.tryCompleteWith(this)
    p.tryCompleteWith(other)

    p.future
  }

  /**
   * Completes the returned future with the first successfully completed future of the two passed futures this and other.
   * If both futures fail, it fails with the final failing future to prevent any starvation of the program.
   * In the C++ implementation we did rely on the assumption that the promise will be deleted and the resulting future would fail with {@link BrokenPromise}.
   * However, we cannot make such an assumption in this implementation. since Scala has a garbage collection.
   * If we would accept starvation of the program, we could simply use {@link Promise#trySuccess}.
   *
   * @see {@link combinators.Combinators#firstSuccWithOrElse} for a different implementation based on {@link #orElse}.
   */
  def firstSucc(other: Future[T]): Future[T] = {
    val p = factory.createPromise[T]
    /*
     * This context is required to store if both futures have failed to prevent starvation.
     */
    val ctx = new AtomicInteger(0);
    val callback = (t: Try[T]) => {
      if (t.hasException) {
        val c = ctx.incrementAndGet();

        if (c == 2) {
          p.tryComplete(t)
        }
      } else {
        p.trySuccess(t.get())
      }
    }: Unit

    this.onComplete(callback)
    other.onComplete(callback)

    p.future
  }

  /*
   * Derived methods from Scala FP:
   * All methods which have not been part of our C++ paper but which do exist in Scala FP.
   */

  // onComplete does already exist, it is one of our derived methods

  /**
   * Implemented similary to Scala FP's implementation.
   */
  def andThen[U](pf: PartialFunction[Try[T], U]): Future[T] = {
    this.then[T]((t: Try[T]) => {
      try {
        pf.applyOrElse[Try[T], Any](t, Predef.identity(_))
      } catch { case t if NonFatal(t) => }
      t.get()
    })
  }

  /**
   * Our {@link Try} does not have any method foreach.
   * Other than that, this is implemented similiar to Scala FP's implementation
   * This method is bascially onSuccess.
   */
  def foreach[U](f: T => U): Unit = {
    this.onComplete((t: Try[T]) => {
      if (t.hasValue) {
        f(t.get())
      }
    })
  }

  /*
   * Don't implement methods onSuccess and onFailure since they are deprecated.
   */

  // isCompleted is one of our basic methods

  /**
   * This method has to be a basic method and must be implemented by manually since we do not specify how the final result should be stored.
   * The method is basically a non-blocking get.
   */
  def value: Option[Try[T]]

  /**
   * Basically {@link #then} but returns Scala's Try instead of the successful result value or a thrown exception.
   */
  def transform[S](f: (Try[T]) => Try[S]): Future[S] = {
    this.then(t => {
      val r = f.apply(t)

      r.get()
    })
  }

  def transformWith[S](f: (Try[T]) => Future[S]): Future[S] = {
    val p = factory.createPromise[S]

    this.onComplete(t => p.tryCompleteWith(f.apply(t)))

    p.future()
  }

  def collect[S](pf: PartialFunction[T, S]): Future[S] = {
    this.then((t: Try[T]) => {
      val r = t.get()

      pf.applyOrElse(r, (v: T) => throw new NoSuchElementException("Future.collect partial function is not defined at: " + r) with NoStackTrace)
    })
  }

  def failed: Future[Throwable] = {
    this.then((t: Try[T]) => {
      try {
        t.get()
        throw new NoSuchElementException("Future.failed not completed with a throwable.") with NoStackTrace
      } catch {
        case NonFatal(e) => e
      }
    })
  }

  /**
   * Basically {@link #orElse} but with lighter type restrictions.
   * Our {@link #orElse} uses T and not U.
   * If U would be = T we could directly call {@link #orElse} here.
   */
  def fallbackTo[U >: T](that: Future[U]): Future[U] = {
    this.then[U]((t: Try[T]) => {
      if (t.hasException) {
        try {
          that.get
        } catch {
          case NonFatal(x) => t.get // will rethrow if failed
        }
      } else {
        t.get
      }
    })
  }

  /**
   * Basically {@link #guard} but with the exception NoSuchElementException instead of {@link PredicateNotFulfilled}.
   * We could implement this by using {@link #guard} if the type of the exception would not matter.
   */
  def filter(p: (T) => Boolean): Future[T] = {
    this.then[T]((t: Try[T]) => {
      val v: T = t.get() // rethrows the exception if necessary

      if (!p.apply(v)) {
        throw new NoSuchElementException("Future.filter predicate is not satisfied") with NoStackTrace
      }

      v
    })
  }

  /**
   * Scala FP does also implement this with the help of {@link #transformWith}.
   */
  def flatMap[S](f: (T) => Future[S]): Future[S] = transformWith[S](t => f.apply(t.get()))

  /**
   * Scala FP does also implement this with the help of {@link #flatMap}.
   */
  def flatten[S](implicit ev: <:<[T, Future[S]]): Future[S] = flatMap(ev)

  /**
   * Basically {@link #then} but passing only the successful value.
   * Scala FP does implement this with the help of {@link #transform}.
   */
  def map[S](f: (T) => S): Future[S] = this.then(t => f.apply(t.get()))

  /**
   * Scala FP does implement this the same way. Copy&paste.
   */
  def mapTo[S](implicit tag: ClassTag[S]): Future[S] = {
    val boxedClass = {
      val c = tag.runtimeClass
      if (c.isPrimitive) toBoxed(c) else c
    }
    require(boxedClass ne null)
    map(s => boxedClass.cast(s).asInstanceOf[S])
  }

  /**
   * Scala FP does implement this with transform and recover of Scala's Try type.
   */
  def recover[U >: T](pf: PartialFunction[Throwable, U]): Future[U] = {
    this.then(
      (t) => {
        try {
          t.get()
        } catch {
          case NonFatal(x) => pf.applyOrElse(x, throw x)
        }
      })
  }

  /**
   * Other than the Scala FP implementation we try not to use a specific marker future here.
   * Otherwise, we would have to create a failed future from the beginning and store it somewhere.
   * We just check with isDefinedAt if the partial function can be applied.
   * This function requires the type T of Future to be covariant.
   */
  def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]]): Future[U] = {
    transformWith((t: Try[T]) => {
      try {
        t.get
        this
      } catch {
        case NonFatal(x) => {
          if (pf.isDefinedAt(x)) {
            pf.apply(x)
          } else {
            this
          }
        }
      }
    })
  }

  /**
   * Scala FP uses transform for this implementation.
   */
  def transform[S](s: (T) => S, f: (Throwable) => Throwable): Future[S] = {
    this.then(t =>
      {
        try {
          s.apply(t.get())
        } catch {
          case NonFatal(x) => throw f.apply(x)
        }
      })
  }

  /**
   * Scala FP implements this in the same way.
   */
  def withFilter(p: (T) => Boolean): Future[T] = filter(p)

  /**
   * Scala FP implements this the same way but uses some extra method and val for it.
   */
  def zip[U](that: Future[U]): Future[(T, U)] = zipWith(that)(Tuple2.apply)

  /**
   * Scala FP implements this the same way.
   */
  def zipWith[U, R](that: Future[U])(f: (T, U) => R): Future[R] = flatMap(r1 => that.map(r2 => f(r1, r2)))

  // TODO Implement the following methods from Scala FP

  /**
   * Scala FP uses the internal method tryAwait0 to implement these two methods.
   * This method uses a AbstractQueuedSynchronizer from Java to synchronize the completion of the future's result.
   * The AbstractQueuedSynchronizer is adapted, so it can be a callback function for onComplete.
   * It stores the future's resulting Try[T] and calls releaseShared(1) afterwards.
   */
  // abstract def ready(atMost: Duration)(implicit permit: CanAwait): Future.this.type
  // abstract def result(atMost: Duration)(implicit permit: CanAwait): T

  private def tryToScalaUtilTry[T](t: Try[T]): scala.util.Try[T] = {
    try {
      Success(t.get())
    } catch {
      case NonFatal(e) => Failure(e)
    }
  }

  /**
   * Copied from the Scala FP Future object.
   * Required by {@link #mapTo[S](implicit tag: ClassTag[S]).
   */
  private final val toBoxed = Map[Class[_], Class[_]](
    classOf[Boolean] -> classOf[java.lang.Boolean],
    classOf[Byte] -> classOf[java.lang.Byte],
    classOf[Char] -> classOf[java.lang.Character],
    classOf[Short] -> classOf[java.lang.Short],
    classOf[Int] -> classOf[java.lang.Integer],
    classOf[Long] -> classOf[java.lang.Long],
    classOf[Float] -> classOf[java.lang.Float],
    classOf[Double] -> classOf[java.lang.Double],
    classOf[Unit] -> classOf[scala.runtime.BoxedUnit])
}