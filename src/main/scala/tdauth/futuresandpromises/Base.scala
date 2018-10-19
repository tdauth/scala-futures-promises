package tdauth.futuresandpromises

import scala.concurrent.SyncVar
import scala.annotation.tailrec

private[futuresandpromises] trait CallbackEntry {
}

/**
 * Single backwards linked list of callbacks.
 * When appending a callback, it will only be reversed link and the current link will be replaced by the new element.
 * This should improve the performance on appending elements compared to storing the whole list.
 * This does also mean that when the callbacks are called, they will be called in reverse order.
 */
private[futuresandpromises] class LinkedCallbackEntry[T]( final val c: (Try[T]) => Unit, final val prev: LinkedCallbackEntry[T] = null) extends CallbackEntry {
}
/**
 * Indicates that there is no callback.
 */
private[futuresandpromises] final class EmptyCallbackEntry extends CallbackEntry

object Base {
  private[futuresandpromises] final val Noop = new EmptyCallbackEntry
}

/**
 * Primitive set of (promise/future) features.
 */
trait Base[T] {
  type Callback = (Try[T]) => Unit
  type LinkedCallbackEntry = tdauth.futuresandpromises.LinkedCallbackEntry[T]
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

  protected def appendCallback(callbacks: CallbackEntry, c: Callback): CallbackEntry = if (callbacks ne Base.Noop) new LinkedCallbackEntry(c, callbacks.asInstanceOf[LinkedCallbackEntry]) else new LinkedCallbackEntry(c)

  protected def dispatchCallback(v: Try[T], c: Callback) = getExecutor.submit(() => c.apply(v))

  protected def dispatchCallbacks(v: Try[T], callbacks: CallbackEntry) = if (callbacks ne Base.Noop) getExecutor.submit(() => applyCallbacks(v, callbacks.asInstanceOf[LinkedCallbackEntry]))

  @tailrec protected final def applyCallbacks(v: Try[T], callbackEntry: LinkedCallbackEntry) {
    callbackEntry.c.apply(v)
    if (callbackEntry.prev ne null) applyCallbacks(v, callbackEntry.prev)
  }

  /**
   * This version is much simpler than the CompletionLatch from Scala FP's implementation.
   * TODO #23 But does it allow interrupts? Is it necessary to allow them?
   */
  private final class CompletionSyncVar[T] extends SyncVar[Try[T]] with (Try[T] => Unit) {
    override def apply(value: Try[T]): Unit = put(value)
  }
}