package tdauth.futuresandpromises.mvar

import scala.concurrent.SyncVar
import scala.util.Left

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.FP
import tdauth.futuresandpromises.Base
import tdauth.futuresandpromises.Try

class PrimMVar[T](ex: Executor) extends SyncVar[FP[T]#Value] with FP[T] {
  put(Right(Base.Noop))

  /*
   * We need a second MVar to signal that the future has a result.
   */
  val sig = new SyncVar[Unit]

  override def getExecutor: Executor = ex

  override def newP[S](ex: Executor): Base[S] = new PrimMVar[S](ex)

  override def getP: T = {
    sig.get
    get.left.get.get()
  }

  /**
   * In Haskell we could call isEmptyMVar.
   */
  override def isReady: Boolean = sig.isSet

  override def tryComplete(v: Try[T]): Boolean = {
    val s = take()
    s match {
      case Left(_) => {
        // Put the value back.
        put(s)
        false
      }
      case Right(x) => {
        put(Left(v))
        sig.put()
        dispatchCallbacks(v, x)
        true
      }
    }
  }

  override def onComplete(c: Callback): Unit = {
    val s = take()
    s match {
      case Left(x) => {
        put(s)
        dispatchCallback(x, c)
      }
      case Right(x) => put(Right(appendCallback(x, c)))
    }
  }
}