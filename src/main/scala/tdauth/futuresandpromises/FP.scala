package tdauth.futuresandpromises

import java.util.concurrent.atomic.AtomicInteger

import scala.util.control.NonFatal

/**
 * Combines the functionality of futures and promises and relies on some basic abstract methods.
 * The rest of the methods is derived from these basic methods.
 *
 * Based on this Haskell definition:
 * ```haskell
 * class FP t where
 *
 * new :: IO (t a)
 * trySuccess :: t a -> a -> IO Bool
 * tryFail :: t a -> IO Bool
 * tryComplete :: t a -> IO (Maybe a) -> IO Bool
 * trySuccWith :: t a -> t a -> IO ()
 * tryFailWith :: t a -> t a -> IO ()
 * tryCompleteWith :: t a -> t a -> IO ()
 *
 * future :: (() -> IO (Maybe a)) -> IO (t a)
 * get :: t a -> IO (Maybe a)
 * onComplete :: t a -> (Maybe a -> IO ()) -> IO ()
 * onSuccess :: t a -> (a -> IO ()) -> IO ()
 * onFail :: t a -> (() -> IO ()) -> IO ()
 *
 * followedBy :: t a -> (a -> IO (Maybe b)) -> IO (t b)
 * followedByWith :: t a -> (a -> IO (t b)) -> IO (t b)
 * guard :: t a -> (a -> IO Bool) -> IO (t a)
 * orAlt :: t a -> t a -> IO (t a)
 * first :: t a -> t a -> IO (t a)
 * firstSucc :: t a -> t a -> IO (t a)
 * ```
 * Combines a Prim, a future and a promise to one single trait.
 */
trait FP[T] extends Prim[T] {
  // Basic construction methods:
  def newFP[S]: FP[S] = newP[S].asInstanceOf[FP[S]]

  // Basic promise methods:
  // getExecutor is inherited by Prim[T]
  def tryComplete(t: Try[T]): Boolean

  // Basic future methods:
  // TODO #25 isReady is missing from Haskell?
  // isReady is inherited by Prim[T]
  // TODO #25 Do we even need get? It is only for testing purposes -> explicit blocking.
  def get: T = getP
  // onComplete is inherited by Prim[T]

  // Derived promise methods:
  def trySuccess(v: T): Boolean = tryComplete(new Try(v))
  def tryFail(e: Throwable): Boolean = tryComplete(new Try(e))
  def tryCompleteWith(other: FP[T]): Unit = other.onComplete(this.tryComplete(_))
  // TODO #25 Why call it trySuccWith in Haskell? -> trySuccessWith
  def trySuccessWith(other: FP[T]): Unit = other.onSuccess(this.trySuccess(_))
  def tryFailWith(other: FP[T]): Unit = other.onFail(this.tryFail(_))

  // Derived future methods:
  // TODO #25 async? static method?
  def future[S](f: () => S): FP[S] = {
    val p = newFP[S]
    getExecutor.submit(() => {
      try {
        p.trySuccess(f.apply())
      } catch {
        case NonFatal(e) => p.tryFail(e)
      }
    })

    p
  }
  def onSuccess(f: T => Unit): Unit = onComplete(t => if (t.hasValue) f.apply(t.get))
  def onFail(f: Throwable => Unit): Unit = onComplete(t => if (t.hasException) f.apply(t.getException.get))
  // TODO #25 What about transform and transformWith? orAlt requires this and followedBy and followedbyWith can be derived?
  // TODO #25 In Scala FP transform and transformWith return Try[T] and not T. Mention that!
  def transform[S](f: Try[T] => S): FP[S] = {
    val p = newFP[S]
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
    val p = newFP[S]
    onComplete(t => p.tryCompleteWith(f.apply(t)))
    p
  }
  def followedBy[S](f: T => S): FP[S] = transform(t => f.apply(t.get))
  def followedByWith[S](f: T => FP[S]): FP[S] = transformWith(t =>
    try {
      f.apply(t.get)
    } catch {
      case NonFatal(x) => {
        val p = newFP[S]
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
  def orAlt(other: FP[T]): FP[T] = transformWith(t => if (t.hasValue) this else other.transform(tt =>
    if (tt.hasValue) {
      tt.get
    } else {
      t.get
    }))

  def first(other: FP[T]): FP[T] = {
    val p = newFP[T]
    p.tryCompleteWith(this)
    p.tryCompleteWith(other)

    p
  }
  def firstSucc(other: FP[T]): FP[T] = {
    val p = newFP[T]
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