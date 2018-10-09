package tdauth.futuresandpromises.mvar

import scala.concurrent.SyncVar
import scala.util.Left

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.FP
import tdauth.futuresandpromises.Prim
import tdauth.futuresandpromises.Try

class PrimMVar[T](ex: Executor) extends FP[T] {
  type Result = SyncVar[Value]

  var result = new Result()
  result.put(Right(List.empty[Callback]))
  /*
   * We need a second MVar to signal that the future has a result.
   */
  val sig = new SyncVar[Unit]

  override def getExecutor: Executor = ex

  override def newP[S](ex: Executor): Prim[S] = new PrimMVar[S](ex)

  override def getP: T = {
    sig.get
    result.get.left.get.get()
  }

  /**
   * In Haskell we could call isEmptyMVar.
   */
  override def isReady: Boolean = sig.isSet

  override def tryComplete(v: Try[T]): Boolean = {
    val s = result.take()
    s match {
      case Left(_) => {
        // Put the value back.
        result.put(s)
        false
      }
      case Right(x) => {
        result.put(Left(v))
        sig.put()
        dispatchCallbacks(v, x)
        true
      }
    }
  }

  override def onComplete(c: Callback): Unit = {
    val s = result.take()
    s match {
      case Left(x) => dispatchCallback(x, c)
      case Right(x) => {
        result.put(Right(x :+ c))
      }
    }
  }
}