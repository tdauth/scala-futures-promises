package tdauth.futuresandpromises.core

import tdauth.futuresandpromises._

import scala.concurrent.SyncVar

/**
  * Primitive set of (promise/future) features.
  */
trait Core[T] {

  type Callback = (Try[T]) => Unit

  type Value = Either[Try[T], CallbackEntry]

  def newC[S](ex: Executor): Core[S]

  /**
    * The executor is passed on the combined futures.
    */
  def getExecutorC: Executor

  /**
    * Blocks until the future has been completed and returns the successful result value or throws the failing exception.
    */
  def getC: T
  def tryCompleteC(v: Try[T]): Boolean
  def onCompleteC(c: Callback): Unit
  def isReadyC: Boolean

  /**
    * Helper method which uses an MVar to block until the future has been completed and
    * returns its result. Throws an exception if it has failed.
    */
  protected def getResultWithMVar: T = {
    val s = new CompletionSyncVar[T]
    this.onCompleteC(s)
    s.take().get()
  }

  protected def appendCallback(callbacks: CallbackEntry, c: Callback): CallbackEntry =
    if (callbacks ne Noop) LinkedCallbackEntry(c, callbacks) else SingleCallbackEntry(c)

  protected def appendCallbacks(callbacks: CallbackEntry, appendedCallbacks: CallbackEntry): CallbackEntry =
    if (callbacks ne Noop) { ParentCallbackEntry(appendedCallbacks, callbacks) } else { appendedCallbacks }

  protected def dispatchCallback(v: Try[T], c: Callback): Unit = getExecutorC.submit(() => c.apply(v))

  /**
    * Dispatches all callbacks together at once to the executor.
    */
  protected def dispatchCallbacks(v: Try[T], callbacks: CallbackEntry): Unit =
    if (callbacks ne Noop) getExecutorC.submit(() => applyCallbacks(v, callbacks))

  // TODO #32 @tailrec
  // TODO #32 How to do pattern matching with keeping the generic types T?
  protected final def applyCallbacks(v: Try[T], callbackEntry: CallbackEntry) {
    callbackEntry match {
      case LinkedCallbackEntry(_, prev) => {
        callbackEntry.asInstanceOf[LinkedCallbackEntry[T]].c.apply(v)
        applyCallbacks(v, prev)
      }
      case SingleCallbackEntry(_) => callbackEntry.asInstanceOf[SingleCallbackEntry[T]].c.apply(v)
      case ParentCallbackEntry(left, right) => {
        applyCallbacks(v, left) // TODO #32 Make one list from them which does still allow tail call optimization. Scala 2.13.xx has a FIXME here, too but for their type ManyCallbacks.
        applyCallbacks(v, right)
      }
      case Noop =>
    }
  }

  /**
    * Dispatches each callback separately to the executor.
    */
  // TODO #32 @tailrec
  // TODO #32 How to do pattern matching with keeping the generic types T?
  protected final def dispatchCallbacksOneAtATime(v: Try[T], callbacks: CallbackEntry): Unit = if (callbacks ne Noop) {
    callbacks match {
      case LinkedCallbackEntry(_, prev) => {
        getExecutorC.submit(() => callbacks.asInstanceOf[LinkedCallbackEntry[T]].c.apply(v))
        dispatchCallbacksOneAtATime(v, prev)
      }
      case SingleCallbackEntry(_) => getExecutorC.submit(() => callbacks.asInstanceOf[SingleCallbackEntry[T]].c.apply(v))
      case ParentCallbackEntry(left, right) => {
        dispatchCallbacksOneAtATime(v, left) // TODO #32 Make one list from them which does still allow tail call optimization. Scala 2.13.xx has a FIXME here, too but for their type ManyCallbacks.
        dispatchCallbacksOneAtATime(v, right)
      }
      case Noop =>
    }
  }

  /**
    * This version is much simpler than the CompletionLatch from Scala FP's implementation.
    * TODO #23 But does it allow interrupts? Is it necessary to allow them?
    */
  private final class CompletionSyncVar[T] extends SyncVar[Try[T]] with (Try[T] => Unit) {
    override def apply(value: Try[T]): Unit = put(value)
  }
}
