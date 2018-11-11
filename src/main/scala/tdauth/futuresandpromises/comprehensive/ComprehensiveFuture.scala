package tdauth.futuresandpromises.comprehensive

import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.AbstractQueuedSynchronizer

import scala.concurrent.Awaitable
import scala.concurrent.CanAwait
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.Util

/**
 * This trait provides all of our Advanced Futures and Promises functionality + all methods provided by Scala FP's Future trait.
 */
trait ComprehensiveFuture[T] extends Future[T] with Awaitable[T] {
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
    val p = factory.createPromise[S](getExecutor)

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
   * Scala FP does implement this with the help of transform and uses the map method of Try.
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
   * The Scala FP implementation simply returns this since of Future is covariant in Scala FP.
   * Instead, we construct a new future from a promise which is completed immediately.
   */
  def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]]): Future[U] = {
    transformWith((t: Try[T]) => {
      try {
        val p = factory.createPromise[U](getExecutor)
        p.trySuccess(t.get)
        p.future
      } catch {
        case NonFatal(x) => {
          if (pf.isDefinedAt(x)) {
            pf.apply(x)
          } else {
            val p = factory.createPromise[U](getExecutor)
            p.tryFailure(x)
            p.future
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
  def zipWith[U, R](that: Future[U])(f: (T, U) => R): Future[R] = flatMap(r1 => that.asInstanceOf[ComprehensiveFuture[U]].map(r2 => f(r1, r2)))

  /**
   * These two methods are inherited from the trait Awaitable.
   * Therefore, this trait would have to extend the trait Awaitable to implement these two methods.
   * Note that these methods should never be called directly but with with Await.ready and Await.result.
   * The object Await is defined in the file "package.scala".
   * Scala FP uses the internal method tryAwait0 to implement these two methods.
   * This method uses a AbstractQueuedSynchronizer from Java to synchronize the completion of the future's result.
   * The AbstractQueuedSynchronizer is adapted, so it can be a callback function for onComplete.
   * It stores the future's resulting Try[T] and calls releaseShared(1) afterwards.
   *
   * We can implement this the same except for an immediate check for value0 since we cannot check the current value of the future.
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  def ready(atMost: Duration, ex: Executor)(implicit permit: CanAwait): ComprehensiveFuture.this.type = {
    tryAwait0(atMost)
    this
  }

  @throws(classOf[Exception])
  def result(atMost: Duration)(implicit permit: CanAwait): T = tryAwait0(atMost).get // returns the value, or throws the contained exception

  /**
   * Copied from Scala FP Promise object.
   */
  /**
   * Latch used to implement waiting on a DefaultPromise's result.
   *
   * Inspired by: http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/locks/AbstractQueuedSynchronizer.java
   * Written by Doug Lea with assistance from members of JCP JSR-166
   * Expert Group and released to the public domain, as explained at
   * http://creativecommons.org/publicdomain/zero/1.0/
   */
  private final class CompletionLatch[T] extends AbstractQueuedSynchronizer with (Try[T] => Unit) {
    //@volatie not needed since we use acquire/release
    /*@volatile*/ private[this] var _result: Try[T] = null
    final def result: Try[T] = _result
    override protected def tryAcquireShared(ignored: Int): Int = if (getState != 0) 1 else -1
    override protected def tryReleaseShared(ignore: Int): Boolean = {
      setState(1)
      true
    }
    override def apply(value: Try[T]): Unit = {
      _result = value // This line MUST go before releaseShared
      releaseShared(1)
    }
  }

  private[this] final def tryAwait0(atMost: Duration): Try[T] = {
    if (atMost ne Duration.Undefined) {
      val r =
        if (atMost <= Duration.Zero) null
        else {
          val l = new CompletionLatch[T]()
          this.onComplete(l)

          if (atMost.isFinite)
            l.tryAcquireSharedNanos(1, atMost.toNanos)
          else
            l.acquireSharedInterruptibly(1)

          l.result
        }
      if (r ne null) r
      else throw new TimeoutException("Future timed out after [" + atMost + "]")

    } else throw new IllegalArgumentException("Cannot wait for Undefined duration of time")
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

/**
 * Our implementation requires a factory.
 * Hence, it has to be passed as parameter.
 * The factory could be used directly instead.
 * The object would only make sense in a concrete implementation such as the one based on Scala FP where the factory would not be necessary.
 */
object ComprehensiveFuture {
  final def apply[T](f: Factory, ex: Executor, body: => T): Future[T] = unit(f, ex).asInstanceOf[ComprehensiveFuture[T]].map(_ => body)

  /**
   * Scala FP implements this the same way. Copy & paste.
   * TODO #15 This does seem to exist in Scala 2.13.x but not in Scala 2.12.x
   */
  final def delegate[T](f: Factory, ex: Executor, body: => Future[T]): Future[T] = unit(f, ex).asInstanceOf[ComprehensiveFuture[T]].flatMap(_ => body)

  final def failed[T](f: Factory, ex: Executor, exception: Throwable): Future[T] = {
    val p = f.createPromise[T](ex)
    p.tryFailure(exception)
    p.future
  }

  /**
   * Scala FP implements this the same way. Copy&Paste with some minor modifications.
   */
  final def find[T](f: Factory, ex: Executor, futures: scala.collection.immutable.Iterable[Future[T]])(p: T => Boolean): Future[Option[T]] = {
    def searchNext(i: Iterator[Future[T]]): Future[Option[T]] =
      if (!i.hasNext) successful[Option[T]](f, ex, None)
      else {
        i.next().asInstanceOf[ComprehensiveFuture[T]].transformWith((t) => {

          try {
            val r = t.get

            if (p(r)) {
              successful(f, ex, Some(r))
            } else {
              searchNext(i)
            }
          } catch {
            case NonFatal(e) => searchNext(i)
          }
        })
      }
    searchNext(futures.iterator)
  }

  /**
   * We can simply use {@link Util#firstN} with a value of 1.
   * Scala FP has to implement this manually.
   */
  final def firstCompletedOf[T](ex: Executor, u: Util, futures: TraversableOnce[Future[T]]): Future[T] = u.firstN(ex, futures.toVector, 1).then(t => t.get().apply(0)._2.get())

  // TODO #15 Implement foldLeft

  final def fromTry[T](f: Factory, ex: Executor, result: Try[T]): Future[T] = {
    val p = f.createPromise[T](ex)
    p.tryComplete(result)
    p.future
  }

  // TODO #15 Implement reduceLeft
  // TODO #15 Implement sequence

  final def successful[T](f: Factory, ex: Executor, result: T): Future[T] = {
    val p = f.createPromise[T](ex)
    p.trySuccess(result)
    p.future
  }

  // TODO #15 Implement traverse

  /**
   * This has to be a method here since it requires a factory, too.
   */
  final def unit(f: Factory, ex: Executor): Future[Unit] = fromTry(f, ex, new Try[Unit]())

  // TODO #15 Implement never
}