package tdauth.futuresandpromises.cas
import java.util.concurrent.atomic.AtomicReference

import tdauth.futuresandpromises._

import scala.annotation.tailrec

sealed trait ValueType[T]
case class ValueTypeTry[T](t: Try[T]) extends ValueType[T]
case class ValueTypeCallbackEntry[T](c: CallbackEntry) extends ValueType[T]
case class ValueTypeLink[T](l: PrimCASPromiseLinking[T]) extends ValueType[T]

/**
  * Similar to [[PrimCAS]] but implements [[FP#tryCompleteWith]] with the help of promise linking optimization (implemented in Twitter Util and Scala FP).
  * Whenever two promises are equal, all callbacks are moved to one of them.
  * TODO #32 Implement link compression. Whenever getting the root of chain of links, compress the chain which means moving all callbacks to the left promise and linking all links to the left.
  * This is done by Scala FP to reduce the number of existing promises.
  */
class PrimCASPromiseLinking[T](ex: Executor) extends AtomicReference[ValueType[T]](ValueTypeCallbackEntry[T](CallbackEntry.Noop)) with FP[T] {

  override def getExecutor: Executor = ex

  override def newP[S](ex: Executor): Base[S] = new PrimCASPromiseLinking[S](ex)

  override def getP: T = super[FP].getResultWithMVar

  override def isReady: Boolean = {
    val s = get
    s match {
      case ValueTypeTry(_)           => true
      case ValueTypeCallbackEntry(_) => false
      case ValueTypeLink(x)          => x.isReady
    }
  }

  override def tryComplete(v: Try[T]): Boolean = tryCompleteInternal(v)

  override def onComplete(c: Callback): Unit = onCompleteInternal(c)

  override def tryCompleteWith(other: FP[T]): Unit = tryCompleteWithInternal(other)

  @tailrec private def tryCompleteInternal(v: Try[T]): Boolean = {
    val s = get
    s match {
      case ValueTypeTry(_) => false
      case ValueTypeCallbackEntry(x) => {
        if (compareAndSet(s, ValueTypeTry(v))) {
          dispatchCallbacks(v, x)
          true
        } else {
          tryCompleteInternal(v)
        }
      }
      case ValueTypeLink(l) => l.tryComplete(v)
    }
  }

  @tailrec private def onCompleteInternal(c: Callback): Unit = {
    val s = get
    s match {
      case ValueTypeTry(x)           => dispatchCallback(x, c)
      case ValueTypeCallbackEntry(x) => if (!compareAndSet(s, ValueTypeCallbackEntry(appendCallback(x, c)))) onCompleteInternal(c)
      case ValueTypeLink(l)          => l.onComplete(c)
    }
  }

  /**
    * Creates a link from other to this and moves all callbacks to this.
    * The callback list of other is replaced by a link.
    */
  @tailrec private def tryCompleteWithInternal(other: FP[T]): Unit = {
    if (other.isInstanceOf[PrimCASPromiseLinking[T]]) {
      val o = other.asInstanceOf[PrimCASPromiseLinking[T]]
      val s = o.get
      s match {
        case ValueTypeTry(x)           => tryComplete(x)
        case ValueTypeCallbackEntry(x) => if (!o.compareAndSet(s, ValueTypeLink(this))) tryCompleteWithInternal(other) else onComplete(x)
        case ValueTypeLink(l)          => tryCompleteWithInternal(l)
      }
    } else {
      other.onComplete(this.tryComplete(_))
    }
  }

  /**
    * Appends all callbacks linked with the passed callback entry.
    */
  @tailrec private def onComplete(c: CallbackEntry): Unit = {
    val s = get
    s match {
      case ValueTypeTry(x)           => dispatchCallbacks(x, c)
      case ValueTypeCallbackEntry(x) => if (!compareAndSet(s, ValueTypeCallbackEntry(appendCallbacks(x, c)))) onComplete(c)
      case ValueTypeLink(l)          => l.onComplete(c)
    }
  }

  private[cas] def isLinkTo(primCASPromiseLinking: PrimCASPromiseLinking[T]): Boolean = {
    val s = get
    s match {
      case ValueTypeTry(_)           => false
      case ValueTypeCallbackEntry(_) => false
      case ValueTypeLink(x)          => x == primCASPromiseLinking
    }
  }

  private[cas] def isLink(): Boolean = {
    val s = get
    s match {
      case ValueTypeTry(_)           => false
      case ValueTypeCallbackEntry(_) => false
      case ValueTypeLink(_)          => true
    }
  }

  private[cas] def getLinkTo(): PrimCASPromiseLinking[T] = {
    val s = get
    s match {
      case ValueTypeTry(_)           => throw new RuntimeException("Invalid usage.")
      case ValueTypeCallbackEntry(_) => throw new RuntimeException("Invalid usage.")
      case ValueTypeLink(x)          => x
    }
  }
}
