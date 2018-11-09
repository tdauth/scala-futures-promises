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
  *
  * Here is an example:
  * ```
  * f0 tryCompleteWith f1 // f0 <- f1: f1 becomes a link to f0
  * f1 tryCompleteWith f2 // f1 <- f2: f2 becomes a link to f1 which does already link to f0, so it should become a link to f0, f1 could be released by the GC
  * f2 tryCompleteWith f3 // f3 <- f2: f3 becomes a link to f2 which does already link to f0, so it should become a link to f0, f2 could be released by the GC
  * ...
  * ```
  *
  * Therefore, every `tryCompleteWith` call has to compress the chain and make the link directly link to f0 which is the root promise.
  */
class PrimCASPromiseLinking[T](ex: Executor) extends AtomicReference[ValueType[T]](ValueTypeCallbackEntry[T](CallbackEntry.Noop)) with FP[T] {

  type Self = PrimCASPromiseLinking[T]

  override def getExecutor: Executor = ex

  override def newP[S](ex: Executor): Base[S] = new PrimCASPromiseLinking[S](ex)

  override def getP: T = super[FP].getResultWithMVar

  override def isReady: Boolean = {
    val s = get
    s match {
      case ValueTypeTry(_)           => true
      case ValueTypeCallbackEntry(_) => false
      case ValueTypeLink(_)          => compressRoot().isReady
    }
  }

  override def tryComplete(v: Try[T]): Boolean = tryCompleteInternal(v)

  override def onComplete(c: Callback): Unit = onCompleteInternal(c)

  override def tryCompleteWith(other: FP[T]): Unit = tryCompleteWithInternal(other)

  @inline @tailrec private def tryCompleteInternal(v: Try[T]): Boolean = {
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
      case ValueTypeLink(_) => compressRoot().tryComplete(v)
    }
  }

  @inline @tailrec private def onCompleteInternal(c: Callback): Unit = {
    val s = get
    s match {
      case ValueTypeTry(x)           => dispatchCallback(x, c)
      case ValueTypeCallbackEntry(x) => if (!compareAndSet(s, ValueTypeCallbackEntry(appendCallback(x, c)))) onCompleteInternal(c)
      case ValueTypeLink(_)          => compressRoot().onComplete(c)
    }
  }

  /**
    * Creates a link from other to this/the root of this and moves all callbacks to this.
    * The callback list of other is replaced by a link.
    */
  @inline @tailrec private def tryCompleteWithInternal(other: FP[T]): Unit = {
    if (other.isInstanceOf[Self]) {
      val o = other.asInstanceOf[Self]
      val s = o.get
      s match {
        case ValueTypeTry(x) => tryComplete(x)
        case ValueTypeCallbackEntry(x) => {
          val root = compressRoot()
          // Replace the callback list by a link to the root of the target and append the callbacks to the root.
          if (!o.compareAndSet(s, ValueTypeLink(root))) tryCompleteWithInternal(other) else root.onComplete(x)
        }
        case ValueTypeLink(_) => tryCompleteWithInternal(o.compressRoot())
      }
    } else {
      other.onComplete(this.tryComplete(_))
    }
  }

  /**
    * Appends all callbacks linked with the passed callback entry.
    */
  @inline @tailrec private def onComplete(c: CallbackEntry): Unit = {
    val s = get
    s match {
      case ValueTypeTry(x)           => dispatchCallbacks(x, c)
      case ValueTypeCallbackEntry(x) => if (!compareAndSet(s, ValueTypeCallbackEntry(appendCallbacks(x, c)))) onComplete(c)
      case ValueTypeLink(_)          => compressRoot().onComplete(c)
    }
  }

  /**
    * Checks for the root promise in a linked chain which is not a link itself but has stored a list of callbacks or is already completed.
    * On the way through the chain it sets all links to the root promise.
    * This should reduce the number of intermediate promises in the chain which are all the same and make them available for the garbage collection
    * if they are not refered anywhere else except in the chain of links.
    * TODO #32 Split into the three methods `compressedRoot`, `root` and `link` like Scala 12.x does to allow @tailrec?
    */
  @inline private def compressRoot(): Self = {
    val s = get
    s match {
      case ValueTypeTry(_)           => this
      case ValueTypeCallbackEntry(_) => this
      case ValueTypeLink(l) => {
        val root = l.compressRoot()
        if (!compareAndSet(s, ValueTypeLink(root))) compressRoot() else root
      }
    }
  }

  /**
    * The following methods exist for tests only.
    * @param primCASPromiseLinking The target promise which this should be a direct link to.
    * @return True if this is a direct link to the target promise. Otherwise, false.
    */
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

  private[cas] def isListOfCallbacks(): Boolean = {
    val s = get
    s match {
      case ValueTypeTry(_)           => false
      case ValueTypeCallbackEntry(_) => true
      case ValueTypeLink(_)          => false
    }
  }

  private[cas] def getNumberOfCallbacks(): Int = {
    val s = get
    s match {
      case ValueTypeTry(_)           => throw new RuntimeException("Is not a list of callbacks.")
      case ValueTypeCallbackEntry(x) => getNumberOfCallbacks(x)
      case ValueTypeLink(_)          => throw new RuntimeException("Is not a list of callbacks.")
    }
  }

  private def getNumberOfCallbacks(c: CallbackEntry): Int = c match {
    case SingleCallbackEntry(_)           => 1
    case ParentCallbackEntry(left, right) => getNumberOfCallbacks(left) + getNumberOfCallbacks(right)
    case EmptyCallbackEntry()             => 0
    case LinkedCallbackEntry(_, prev)     => 1 + getNumberOfCallbacks(prev)
  }
}
