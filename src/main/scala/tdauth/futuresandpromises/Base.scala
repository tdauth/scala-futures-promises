package tdauth.futuresandpromises

import scala.concurrent.SyncVar

/**
  * Primitive set of (promise/future) features.
  */
trait Base[T] {

  type Callback = (Try[T]) => Unit

  /**
    * Single backwards linked list of callbacks.
    * When appending a callback, it will only be reversed link and the current link will be replaced by the new element.
    * This should improve the performance on appending elements compared to storing the whole list.
    * This does also mean that when the callbacks are called, they will be called in reverse order.
    */
  private[futuresandpromises] case class LinkedCallbackEntry(final val c: Callback, final val prev: CallbackEntry) extends CallbackEntry

  /**
    * If there is no link to previous callback entry yet, only the callback has to be stored.
    */
  private[futuresandpromises] case class SingleCallbackEntry(final val c: Callback) extends CallbackEntry

  type Value = Either[Try[T], CallbackEntry]

  /**
    * The executor is passed on the combined futures.
    */
  def getExecutor: Executor

  /**
    * Creates a new primitive with the current executor.
    */
  def newP[S](ex: Executor): Base[S]

  /**
    * Blocks until the future has been completed and returns the successful result value or throws the failing exception.
    */
  def getP: T
  def isReady: Boolean
  def tryComplete(v: Try[T]): Boolean
  def onComplete(c: Callback): Unit

  /**
    * Helper method which uses an MVar to block until the future has been completed and
    * returns its result. Throws an exception if it has failed.
    */
  protected def getResultWithMVar: T = {
    val s = new CompletionSyncVar[T]
    this.onComplete(s)
    s.take().get()
  }

  protected def appendCallback(callbacks: CallbackEntry, c: Callback): CallbackEntry =
    if (callbacks ne CallbackEntry.Noop) LinkedCallbackEntry(c, callbacks) else SingleCallbackEntry(c)

  protected def appendCallbacks(callbacks: CallbackEntry, appendedCallbacks: CallbackEntry): CallbackEntry =
    if (callbacks ne CallbackEntry.Noop) ParentCallbackEntry(appendedCallbacks, callbacks)
    else appendedCallbacks

  protected def dispatchCallback(v: Try[T], c: Callback): Unit = getExecutor.submit(() => c.apply(v))

  /**
    * Dispatches all callbacks together at once to the executor.
    */
  protected def dispatchCallbacks(v: Try[T], callbacks: CallbackEntry): Unit =
    if (callbacks ne CallbackEntry.Noop) getExecutor.submit(() => applyCallbacks(v, callbacks))

  // TODO #32 @tailrec
  protected final def applyCallbacks(v: Try[T], callbackEntry: CallbackEntry) {
    callbackEntry match {
      case LinkedCallbackEntry(c, prev) => {
        c.apply(v)
        applyCallbacks(v, prev)
      }
      case SingleCallbackEntry(c) => c.apply(v)
      case ParentCallbackEntry(left, right) => {
        applyCallbacks(v, left) // TODO #32 Make one list from them which does still allow tail call optimization. Scala 2.13.xx has a FIXME here, too but for their type ManyCallbacks.
        applyCallbacks(v, right)
      }
      case EmptyCallbackEntry() =>
    }
  }

  // TODO #32 @tailrec
  protected final def dispatchCallbacksOneAtATime(v: Try[T], callbacks: CallbackEntry): Unit = if (callbacks ne CallbackEntry.Noop) {
    callbacks match {
      case LinkedCallbackEntry(c, prev) => {
        getExecutor.submit(() => c.apply(v))
        dispatchCallbacksOneAtATime(v, prev)
      }
      case SingleCallbackEntry(c) => getExecutor.submit(() => c.apply(v))
      case ParentCallbackEntry(left, right) => {
        dispatchCallbacksOneAtATime(v, left) // TODO #32 Make one list from them which does still allow tail call optimization. Scala 2.13.xx has a FIXME here, too but for their type ManyCallbacks.
        dispatchCallbacksOneAtATime(v, right)
      }
      case EmptyCallbackEntry() =>
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
