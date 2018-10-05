package tdauth.futuresandpromises.stm

import scala.concurrent.stm._

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Try
import scala.concurrent.duration.Duration
import java.util.concurrent.locks.AbstractQueuedSynchronizer
import java.util.concurrent.TimeoutException
import tdauth.futuresandpromises.Prim

class StmSharedState[T](ex: Executor) extends Prim[T] {

  var result: Ref[Value] = Ref(Right(List.empty[Callback]))

  override def getEx: Executor = ex

  override def getP: T = atomic { implicit txn =>
    val s = result()
    s match {
      case Left(x) => x.get
      case Right(x) => retry
    }
  }

  override def isReady: Boolean = {
    atomic { implicit txn =>
      val s = result()
      s match {
        case Left(_) => true
        case Right(_) => false
      }
    }
  }

  override def tryComplete(v: Try[T]): Boolean = {
    atomic { implicit txn =>
      val s = result()
      s match {
        case Left(x) => false
        case Right(x) => {
          result() = Left(v)
          dispatchCallbacks(v, x)
          true
        }
      }
    }
  }

  override def onComplete(c: Callback): Unit = {
    atomic { implicit txn =>
      val s = result()
      s match {
        case Left(x) => dispatchCallback(x, c)
        case Right(x) => {
          result() = Right(x :+ c)
        }
      }
    }
  }
}