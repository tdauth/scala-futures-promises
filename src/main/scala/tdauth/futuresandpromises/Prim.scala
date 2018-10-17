package tdauth.futuresandpromises

import scala.concurrent.SyncVar
import scala.annotation.tailrec

trait CallbackEntry {
}

/**
 * Backwards linked list of callbacks.
 * This should improve the performance on appending elements.
 */
final class LinkedCallbackEntry[T]( final val c: (Try[T]) => Unit, final val previous: LinkedCallbackEntry[T]) extends CallbackEntry {
}
final class EmptyCallbackEntry extends CallbackEntry

object Prim {
  final val Noop = new EmptyCallbackEntry
}

/**
 * Primitive set of (promise/future) features.
 * TODO Directly extend this by the Future and implement future and promise together?
 */
trait Prim[T] {
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
  def newP[S](ex: Executor): Prim[S]
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

  protected def appendCallback(callbacks: CallbackEntry, c: Callback): LinkedCallbackEntry = if (callbacks.isInstanceOf[LinkedCallbackEntry]) new LinkedCallbackEntry(c, callbacks.asInstanceOf[LinkedCallbackEntry]) else new LinkedCallbackEntry(c, null)

  protected def dispatchCallback(v: Try[T], c: Callback) = getExecutor.submit(() => c.apply(v))

  protected def dispatchCallbacks(v: Try[T], callbacks: CallbackEntry) = {
    if (callbacks.isInstanceOf[LinkedCallbackEntry]) {
      getExecutor.submit(() => {
        applyCallbacks(v, callbacks.asInstanceOf[LinkedCallbackEntry])
      })
    }
  }

  // TODO calls the list of callbacks in reverse order
  @tailrec protected final def applyCallbacks(v: Try[T], callbackEntry: LinkedCallbackEntry) {
    callbackEntry.c.apply(v)
    if (callbackEntry.previous ne null) applyCallbacks(v, callbackEntry.previous.asInstanceOf[LinkedCallbackEntry])
  }

  /**
   * This version is much simpler than the CompletionLatch from Scala FP's implementation.
   * TODO #23 But does it allow interrupts? Is it necessary to allow them?
   */
  private final class CompletionSyncVar[T] extends SyncVar[Try[T]] with (Try[T] => Unit) {
    override def apply(value: Try[T]) = put(value)
  }
}