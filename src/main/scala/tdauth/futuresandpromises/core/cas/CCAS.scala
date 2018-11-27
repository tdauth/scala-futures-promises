package tdauth.futuresandpromises.core.cas

import java.util.concurrent.atomic.AtomicReference

import tdauth.futuresandpromises._
import tdauth.futuresandpromises.core.{Core, FP, Noop}

import scala.annotation.tailrec
import scala.util.Left

/**
  * Stores either a result of a future when the future has been completed or the list of callbacks.
  * Thread-safety by CAS operations.
  * This is similiar to Scala FP's implementation.
  */
class CCAS[T](ex: Executor) extends AtomicReference[FP[T]#Value](Right(Noop)) with FP[T] {

  override def getExecutorC: Executor = ex

  override def newC[S](ex: Executor): Core[S] = new CCAS[S](ex)

  override def getC: T = super[FP].getResultWithMVar

  override def isReadyC: Boolean = {
    val s = get
    s match {
      case Left(_)  => true
      case Right(_) => false
    }
  }

  override def tryCompleteC(v: Try[T]): Boolean = tryCompleteInternal(v)

  override def onCompleteC(c: Callback): Unit = onCompleteInternal(c)

  @tailrec private def tryCompleteInternal(v: Try[T]): Boolean = {
    val s = get
    s match {
      case Left(_) => false
      case Right(x) => {
        if (compareAndSet(s, Left(v))) {
          dispatchCallbacksOneAtATime(v, x)
          true
        } else {
          tryCompleteInternal(v)
        }
      }
    }
  }

  @tailrec private def onCompleteInternal(c: Callback): Unit = {
    val s = get
    s match {
      case Left(x)  => dispatchCallback(x, c)
      case Right(x) => if (!compareAndSet(s, Right(appendCallback(x, c)))) onCompleteInternal(c)
    }
  }
}
