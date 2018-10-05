package tdauth.futuresandpromises.cas

import java.util.concurrent.atomic.AtomicReference

import scala.util.Left

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Try
import scala.concurrent.duration.Duration
import scala.concurrent.SyncVar
import tdauth.futuresandpromises.Prim

/**
 * CAS-based shared state.
 *
 * Stores either a result of a future when the future has been completed or the list of callbacks.
 * Thread-safety by CAS operations.
 * This is similiar to Scala FP's implementation.
 *
 * TODO #21 Instead of Either we could use our own Try?
 */
class CasSharedState[T](ex: Executor) extends Prim[T] {
  type Result = AtomicReference[Value]

  var result = new Result(Right(List.empty[Callback]))

  override def getEx: Executor = ex

  override def getP: Value = result.get

  override def tryComplete(v: Try[T]): Boolean = {
    val s = getP
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

  override def onComplete(c: Callback): Unit = {
    val s = getP
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