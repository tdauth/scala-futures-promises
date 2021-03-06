package tdauth.futuresandpromises.core

import java.util.concurrent.atomic.AtomicInteger

import tdauth.futuresandpromises.{Executor, PredicateNotFulfilled, Try}

import scala.util.control.NonFatal

/**
  * Combines the functionality of futures and promises and relies on some abstract core methods.
  * All concrete methods are derived from these methods of [[Core]].
  */
trait FP[T] extends Core[T] {

  // Basic methods:
  // newC has to be implemented by the concrete types.
  def newFP[S](executor: Executor): FP[S] = newC[S](executor).asInstanceOf[FP[S]]
  def getExecutor: Executor = getExecutorC

  // Basic promise methods:
  def tryComplete(v: Try[T]): Boolean = tryCompleteC(v)

  // Basic future methods:
  def onComplete(c: Callback): Unit = onCompleteC(c)
  // We have to use getP since the name get is already used by AtomicReference.
  def getP: T = getC
  def isReady: Boolean = isReadyC

  // Derived promise methods:
  def trySuccess(v: T): Boolean = tryComplete(new Try(v))
  def tryFail(e: Throwable): Boolean = tryComplete(new Try(e))
  def tryCompleteWith(other: FP[T]): Unit = other.onComplete(this.tryComplete(_))
  def trySuccessWith(other: FP[T]): Unit = other.onSuccess(this.trySuccess(_))
  def tryFailWith(other: FP[T]): Unit = other.onFail(this.tryFail(_))

  // Derived future methods:
  def onSuccess(f: T => Unit): Unit = onComplete(t => if (t.hasValue) f.apply(t.get))
  def onFail(f: Throwable => Unit): Unit = onComplete(t => if (t.hasException) f.apply(t.getException.get))
  // TODO #25 What about transform and transformWith? orAlt requires this and followedBy and followedbyWith can be derived?
  // TODO #25 In Scala FP transform and transformWith return Try[T] and not T. Mention that!
  def transform[S](f: Try[T] => S): FP[S] = {
    val p = newFP[S](getExecutor)
    onComplete(t => {
      try {
        p.trySuccess(f.apply(t))
      } catch {
        case NonFatal(e) => p.tryFail(e)
      }
    })
    p
  }

  def transformWith[S](f: Try[T] => FP[S]): FP[S] = {
    val p = newFP[S](getExecutor)
    onComplete(t => p.tryCompleteWith(f.apply(t)))
    p
  }
  def followedBy[S](f: T => S): FP[S] = transform(t => f.apply(t.get))
  def followedByWith[S](f: T => FP[S]): FP[S] =
    transformWith(t =>
      try {
        f.apply(t.get)
      } catch {
        case NonFatal(x) => {
          val p = newFP[S](getExecutor)
          p.tryFail(x)
          p
        }
    })

  def guard(f: T => Boolean): FP[T] = followedBy(v => if (!f.apply(v)) throw new PredicateNotFulfilled else v)

  /**
    * Actually, orElse or fallbackTo in Scala FP.
    * It is called orAlt since we cannot use orElse in Haskell.
    *
    * We have to use transformWith here to prevent deadlocks.
    * If we would use get inside of the callback, it would register another callback for synchronization with MVars.
    * This callback would not be executed if the executor has only one thread, for example.
    * It would have to be marked as "blocking".
    * With transformWith, the future is directly returned and the callback is not blocked.
    * The same is done in Scala FP.
    */
  def orAlt(other: FP[T]): FP[T] =
    transformWith(
      t =>
        if (t.hasValue) this
        else
          other.transform(tt =>
            if (tt.hasValue) {
              tt.get
            } else {
              t.get
          }))

  def first(other: FP[T]): FP[T] = {
    val p = newFP[T](getExecutor)
    p.tryCompleteWith(this)
    p.tryCompleteWith(other)

    p
  }
  def firstSucc(other: FP[T]): FP[T] = {
    val p = newFP[T](getExecutor)
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

    p
  }
}

object FP {
  // TODO #25 What about async, firstN and firstNSucc?
}
