package tdauth.futuresandpromises

import scala.concurrent.SyncVar

/**
 * Primitive trait to implement a shared state for futures and promises.
 */
trait Prim[T] {
  type Callback = (Try[T]) => Unit
  type Callbacks = scala.collection.immutable.List[Callback]
  type Value = Either[Try[T], Callbacks]

  // TODO #23 How to handle executors?
  def getEx: Executor
  // TODO #23 Handle blocking value separately?
  def getP: Value
  def tryComplete(v: Try[T]): Boolean
  def onComplete(c: Callback): Unit

  def getResult: T = {
    val s = new CompletionSyncVar[T]
    this.onComplete(s)

    s.take().get()
  }

  def isReady: Boolean = {
    val s = getP
    s match {
      case Left(_) => true
      case Right(_) => false
    }
  }

  /// TODO #23 dispatch each callback separately to an executor?
  protected def dispatchCallbacks(v: Try[T], callbacks: Callbacks) = getEx.submit(() => { callbacks.foreach(c => c.apply(v)) })

  protected def dispatchCallback(v: Try[T], c: Callback) = dispatchCallbacks(v, List(c))

  /**
   * This version is much simpler than the CompletionLatch from Scala FP's implementation.
   * TODO #23 But does it allow interrupts? Is it necessary to allow them?
   */
  private final class CompletionSyncVar[T] extends SyncVar[Try[T]] with (Try[T] => Unit) {
    override def apply(value: Try[T]): Unit = {
      put(value)
    }
  }
}