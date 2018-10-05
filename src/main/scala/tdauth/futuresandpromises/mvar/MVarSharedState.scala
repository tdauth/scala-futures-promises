package tdauth.futuresandpromises.mvar

import java.util.concurrent.atomic.AtomicReference

import scala.util.Left

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Try
import scala.concurrent.duration.Duration
import scala.concurrent.SyncVar
import tdauth.futuresandpromises.Prim

class MVarSharedState[T](ex: Executor) extends Prim[T] {
  type Result = SyncVar[Value]

  var result = new Result()
  result.put(Right(List.empty[Callback]))
  /*
   * We need a second MVar to signal that the future has a result.
   */
  val sig = new SyncVar[Unit]

  override def getEx: Executor = ex

  override def getP: T = {
    sig.get
    result.take().left.get.get()
  }

  override def isReady: Boolean = {
    val s = result.take()
    // Put the value back.
    result.put(s)
    s match {
      case Left(_) => true
      case Right(_) => false
    }
  }

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