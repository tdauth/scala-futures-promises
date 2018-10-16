package tdauth.futuresandpromises

import scala.concurrent.SyncVar

/**
 * Primitive set of (promise/future) features.
 * TODO Directly extend this by the Future and implement future and promise together?
 */
trait Prim[T] {
  type Callback = (Try[T]) => Unit
  type Callbacks = scala.collection.immutable.List[Callback]
  type Value = Either[Try[T], Callbacks]

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

  protected def dispatchCallbacks(v: Try[T], callbacks: Callbacks) = if (!callbacks.isEmpty) getExecutor.submit(() => { callbacks.foreach(c => c.apply(v)) })

  protected def dispatchCallback(v: Try[T], c: Callback) = getExecutor.submit(() => { c.apply(v) })

  /**
   * This version is much simpler than the CompletionLatch from Scala FP's implementation.
   * TODO #23 But does it allow interrupts? Is it necessary to allow them?
   */
  private final class CompletionSyncVar[T] extends SyncVar[Try[T]] with (Try[T] => Unit) {
    override def apply(value: Try[T]) = put(value)
  }
}