package tdauth.futuresandpromises

/**
 * Combines a Prim, a future and a promise to one single trait.
 */
trait PrimBasedFutureAndPromise[T] extends Prim[T] with Future[T] with Promise[T] {

  override def future(): Future[T] = this

  override def tryComplete(v: Try[T]): Boolean = tryComplete(v)

  override def get: T = getP

  override def isReady: Boolean = isReady

  override def onComplete(f: (Try[T]) => Unit): Unit = onComplete(f)

  override def getExecutor: Executor = getEx
}