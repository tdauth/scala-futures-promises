package tdauth.futuresandpromises.core.cas

import java.util.concurrent.atomic.AtomicReference

import tdauth.futuresandpromises._

import scala.annotation.tailrec
import scala.util.Left

sealed trait MyCallbackEntry

case class CallbackEntryResult(r: Any) extends MyCallbackEntry

case class DependentCallbackEntry(f: Try[Any] => Unit) extends MyCallbackEntry

case class Result(v: Try[Any]) extends MyCallbackEntry

case class MyCallbackNoop() extends MyCallbackEntry

object MyCallbackEntry {
  val Noop = MyCallbackNoop()
}

/**
  * Th idea of this implementation is that new futures/promises created with [[transform]] and similiar methods will not have their own lists of callbacks
  * Instead, they just link to a position in the list of callbacks of the initial futures/promises.
  * When the future is completed, the list of callbacks will be submitted in correct order and dependent callbacks will call each other.
  *
  *
  * ```scala
  * val f0 = new PrimCASCallbackConcatenations[Int]
  * val f1 = f0.transform(x => x + 1) // The passed callback is inserted into the list of callbacks of f0. f1 is now a link to f0.
  * val f2 = f1.transform(x => x.toString) // The passed callback is inserted into the list of callbacks of f0 before the previously added callback, to be called afterwards. f2 is now a link to f0 at the specific place for the callbacks.
  * ```
  *
  * @param ex
  * @tparam T
  */
/*
class CCASCallbackConcatenations[T](ex: Executor) extends AtomicReference[Either[Try[T], MyCallbackEntry]](Right(MyCallbackEntry.Noop)) with FP[T] {
  type Self = PrimCASCallbackConcatenations[T]

  case class Link[S](to: Self, callbackIndex: Int) extends FP[T]

  override def getExecutor: Executor = ex

  override def isReady: Boolean = {
    val s = get
    s match {
      case Left(_)  => true
      case Right(_) => false
    }
  }

  override def tryComplete(v: Try[T]): Boolean = tryCompleteInternal(v)

  override def onComplete(c: Callback): Unit = onCompleteInternal(c)

  @tailrec private def tryCompleteInternal(v: Try[T]): Boolean = {
    val s = get
    s match {
      case Left(_) => false
      case Right(x) => {
        if (compareAndSet(s, Left(v))) {
          dispatchCallbacks(v, x)
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

  override def transform[S](f: Try[T] => S): FP[S] = {
    val r = Link[S](this)

  }

  @tailrec private def onCompleteInternal[B](c: Try[B] => Unit): Unit = {

    val s = get
    s match {
      case Left(x) => dispatchCallback(x, c)
      case Right(x) => {
        if (!compareAndSet(s, Right(appendCallback(x, c)))) onCompleteInternal(c)
      }
    }
  }
}
*/