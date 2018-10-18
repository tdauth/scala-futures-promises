package tdauth.futuresandpromises.cas

import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.util.Left

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.FP
import tdauth.futuresandpromises.Prim
import tdauth.futuresandpromises.Try

/**
 * Stores either a result of a future when the future has been completed or the list of callbacks.
 * Thread-safety by CAS operations.
 * This is similiar to Scala FP's implementation.
 */
class PrimCAS[T](ex: Executor) extends AtomicReference[FP[T]#Value](Right(Prim.Noop)) with FP[T] {

  override def getExecutor: Executor = ex

  override def newP[S](ex: Executor): Prim[S] = new PrimCAS[S](ex)

  override def getP: T = super[FP].getResultWithMVar

  override def isReady: Boolean = {
    val s = get
    s match {
      case Left(_) => true
      case Right(_) => false
    }
  }

  @tailrec override final def tryComplete(v: Try[T]): Boolean = {
    val s = get
    s match {
      case Left(x) => false
      case Right(x) => {
        if (compareAndSet(s, Left(v))) {
          dispatchCallbacks(v, x)
          true
        } else {
          tryComplete(v)
        }
      }
    }
  }

  @tailrec override final def onComplete(c: Callback): Unit = {
    val s = get
    s match {
      case Left(x) => dispatchCallback(x, c)
      case Right(x) => if (!compareAndSet(s, Right(appendCallback(x, c)))) onComplete(c)
    }
  }
}