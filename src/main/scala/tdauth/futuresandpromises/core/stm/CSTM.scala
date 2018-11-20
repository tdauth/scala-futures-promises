package tdauth.futuresandpromises.core.stm

import tdauth.futuresandpromises._
import tdauth.futuresandpromises.core.{Noop, Core, FP}

import scala.concurrent.stm._

class CSTM[T](ex: Executor) extends FP[T] {

  // TODO Is there some way to extend this ref value?
  var result: Ref[Value] = Ref(Right(Noop))

  override def getExecutorC: Executor = ex

  override def newC[S](ex: Executor): Core[S] = new CSTM[S](ex)

  override def getC: T = atomic { implicit txn =>
    val s = result()
    s match {
      case Left(x)  => x.get
      case Right(x) => retry
    }
  }

  override def isReadyC: Boolean = {
    atomic { implicit txn =>
      val s = result()
      s match {
        case Left(_)  => true
        case Right(_) => false
      }
    }
  }

  override def tryCompleteC(v: Try[T]): Boolean = {
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

  override def onCompleteC(c: Callback): Unit = {
    atomic { implicit txn =>
      val s = result()
      s match {
        case Left(x)  => dispatchCallback(x, c)
        case Right(x) => result() = Right(appendCallback(x, c))
      }
    }
  }
}
