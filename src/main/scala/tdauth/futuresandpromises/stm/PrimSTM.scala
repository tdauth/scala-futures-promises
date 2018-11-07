package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises._

import scala.concurrent.stm._

class PrimSTM[T](ex: Executor) extends FP[T] {

  // TODO Is there some way to extend this ref value?
  var result: Ref[Value] = Ref(Right(CallbackEntry.Noop))

  override def getExecutor: Executor = ex

  override def newP[S](ex: Executor): Base[S] = new PrimSTM[S](ex)

  override def getP: T = atomic { implicit txn =>
    val s = result()
    s match {
      case Left(x)  => x.get
      case Right(x) => retry
    }
  }

  override def isReady: Boolean = {
    atomic { implicit txn =>
      val s = result()
      s match {
        case Left(_)  => true
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
        case Left(x)  => dispatchCallback(x, c)
        case Right(x) => result() = Right(appendCallback(x, c))
      }
    }
  }
}
