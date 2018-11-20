package tdauth.futuresandpromises.core.mvar

import tdauth.futuresandpromises._
import tdauth.futuresandpromises.core.{Noop, Core, FP}

import scala.concurrent.SyncVar
import scala.util.Left

class CMVar[T](ex: Executor) extends SyncVar[FP[T]#Value] with FP[T] {
  put(Right(Noop))

  /*
   * We need a second MVar to signal that the future has a result.
   */
  val sig = new SyncVar[Unit]

  override def getExecutorC: Executor = ex

  override def newC[S](ex: Executor): Core[S] = new CMVar[S](ex)

  override def getC: T = {
    sig.get
    get.left.get.get()
  }

  /**
    * In Haskell we could call isEmptyMVar.
    */
  override def isReadyC: Boolean = sig.isSet

  override def tryCompleteC(v: Try[T]): Boolean = {
    val s = take()
    s match {
      case Left(_) => {
        // Put the value back.
        put(s)
        false
      }
      case Right(x) => {
        put(Left(v))
        sig.put(())
        dispatchCallbacks(v, x)
        true
      }
    }
  }

  override def onCompleteC(c: Callback): Unit = {
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
