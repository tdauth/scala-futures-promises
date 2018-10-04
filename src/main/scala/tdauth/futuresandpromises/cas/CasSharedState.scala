package tdauth.futuresandpromises.cas

import java.util.concurrent.atomic.AtomicReference

import scala.util.Left

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Try
import scala.concurrent.duration.Duration
import java.util.concurrent.locks.AbstractQueuedSynchronizer
import java.util.concurrent.TimeoutException

/**
 * CAS-based shared state.
 *
 * Stores either a result of a future when the future has been completed or the list of callbacks.
 * Thread-safety by CAS operations.
 * This is similiar to Scala FP's implementation.
 *
 * TODO #21 Instead of Either we could use our own Try?
 */
class CasSharedState[T](ex: Executor) extends AtomicReference[Either[Try[T], scala.collection.immutable.List[(Try[T]) => Unit]]](Right(scala.collection.immutable.List.empty[(Try[T]) => Unit])) {
  type Callback = (Try[T]) => Unit
  type Callbacks = scala.collection.immutable.List[Callback]
  type Value = Either[Try[T], Callbacks]

  def tryComplete(v: Try[T]): Boolean = {
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

  def onComplete(c: Callback): Unit = {
    val s = get
    s match {
      case Left(x) => dispatchCallback(x, c)
      case Right(x) => {
        val callbacks = x :+ c
        if (!compareAndSet(s, Right(callbacks))) {
          onComplete(c)
        }
      }
    }
  }

  /**
   * Based on the Scala FP implementation.
   */
  def getResult: T = {
    val l = new CompletionLatch[T]()
    this.onComplete(l)
    l.acquireSharedInterruptibly(1)

    l.result.get()
  }

  def isReady: Boolean = {
    val s = get
    s match {
      case Left(_) => true
      case Right(_) => false
    }
  }

  private def dispatchCallbacks(v: Try[T], callbacks: Callbacks) = ex.submit(() => { callbacks.foreach(c => c.apply(v)) })

  private def dispatchCallback(v: Try[T], c: Callback) = ex.submit(() => { c.apply(v) })

  /**
   * Copied from Scala FP Promise object.
   */
  /**
   * Latch used to implement waiting on a DefaultPromise's result.
   *
   * Inspired by: http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/locks/AbstractQueuedSynchronizer.java
   * Written by Doug Lea with assistance from members of JCP JSR-166
   * Expert Group and released to the public domain, as explained at
   * http://creativecommons.org/publicdomain/zero/1.0/
   */
  private final class CompletionLatch[T] extends AbstractQueuedSynchronizer with (Try[T] => Unit) {
    //@volatie not needed since we use acquire/release
    /*@volatile*/ private[this] var _result: Try[T] = null
    final def result: Try[T] = _result
    override protected def tryAcquireShared(ignored: Int): Int = if (getState != 0) 1 else -1
    override protected def tryReleaseShared(ignore: Int): Boolean = {
      setState(1)
      true
    }
    override def apply(value: Try[T]): Unit = {
      _result = value // This line MUST go before releaseShared
      releaseShared(1)
    }
  }
}