package tdauth.futuresandpromises.cas

import java.util.concurrent.atomic.AtomicReference

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
class PrimCAS[T](ex: Executor) extends FP[T] {
  type Result = AtomicReference[Value]

  var result = new Result(Right(List.empty[Callback]))

  override def getExecutor: Executor = ex

  override def newP[S]: Prim[S] = new PrimCAS[S](ex)

  override def getP: T = getResultWithMVar

  override def isReady = {
    val s = result.get
    s match {
      case Left(_) => true
      case Right(_) => false
    }
  }

  // TODO #25 Optimize recursive call?
  override def tryComplete(v: Try[T]): Boolean = {
    val s = result.get
    s match {
      case Left(x) => false
      case Right(x) => {
        if (result.compareAndSet(s, Left(v))) {
          dispatchCallbacks(v, x)
          true
        } else {
          tryComplete(v)
        }
      }
    }
  }

  // TODO #25 Optimize recursive call?
  override def onComplete(c: Callback): Unit = {
    val s = result.get
    s match {
      case Left(x) => dispatchCallback(x, c)
      case Right(x) => {
        val callbacks = x :+ c
        if (!result.compareAndSet(s, Right(callbacks))) {
          onComplete(c)
        }
      }
    }
  }
}