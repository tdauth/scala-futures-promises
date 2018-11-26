package tdauth.futuresandpromises.core.cas
import java.util.concurrent.atomic.AtomicReference

import tdauth.futuresandpromises.{Executor, Try}
import tdauth.futuresandpromises.core._

import scala.annotation.tailrec

sealed trait FixedValueType[T]
case class FixedValueTypeTry[T](t: Try[T]) extends FixedValueType[T]
case class FixedValueTypeCallbackEntry[T](c: CallbackEntry) extends FixedValueType[T]
case class FixedValueTypeLink[T](links: Set[CCASFixedPromiseLinking[T]], c: CallbackEntry) extends FixedValueType[T]

/**
  * Similiar to [[CCASPromiseLinking]] but does not simply move all callbacks to the root promise.
  * It creates a link to the target promise and keeps the current list of callbacks in this link.
  * This prevents all callbacks from being submitted if only one single link is completed.
  * The implementation does still allow one link to link to multiple promises.
  *
  * In the standard case of linking:
  * ```
  * f0 tryCompleteWith f1
  * f1 tryCompleteWith f2
  * ```
  * it will set `f1` and `f2` to links which keep their callbacks separate but know that they have to complete their target, too.
  * When `f2` is finally completed, it will complete `f1` and `f0`, too and collect their callbacks. It will submit all the
  * callbacks at once.
  *
  * TODO This behaviour does not compress the chain since `f2` will still be linked to `f1` and not directly to `f0`. Hence,
  * we have more promises in the chain.
  */
class CCASFixedPromiseLinking[T](ex: Executor) extends AtomicReference[FixedValueType[T]](FixedValueTypeCallbackEntry[T](Noop)) with FP[T] {
  type Self = CCASFixedPromiseLinking[T]

  override def getExecutorC: Executor = ex

  override def newC[S](ex: Executor): Core[S] = new CCASFixedPromiseLinking[S](ex)

  override def getC(): T = super[FP].getResultWithMVar

  override def isReadyC(): Boolean = {
    val s = get
    s match {
      case FixedValueTypeTry(_)                                      => true
      case FixedValueTypeCallbackEntry(_) | FixedValueTypeLink(_, _) => false
    }
  }

  override def tryCompleteC(v: Try[T]): Boolean = tryCompleteInternal(v)

  override def onCompleteC(c: Callback): Unit = onCompleteInternal(c)

  override def tryCompleteWith(other: FP[T]): Unit = tryCompleteWithInternal(other)

  @inline @tailrec private def tryCompleteInternal(v: Try[T]): Boolean = {
    val s = get
    s match {
      case FixedValueTypeTry(_) => false
      case FixedValueTypeCallbackEntry(x) => {
        if (compareAndSet(s, FixedValueTypeTry(v))) {
          dispatchCallbacks(v, x)
          true
        } else {
          tryCompleteInternal(v)
        }
      }
      case FixedValueTypeLink(links, c) => {
        if (compareAndSet(s, FixedValueTypeTry(v))) {

          /**
            * We collect all the callbacks from the target promises by completing them.
            * Already completed promises and promises without callbacks will be ignored.
            */
          val targetCallbacks = tryCompleteAllAndGetCallbacks(links, v)

          /**
            * Submit all collected callbacks together with the ones of this.
            */
          if (targetCallbacks.isDefined) {
            dispatchCallbacks(v, ParentCallbackEntry(c, targetCallbacks.get))
          } else {
            dispatchCallbacks(v, c)
          }
          true
        } else {
          tryCompleteInternal(v)
        }
      }
    }
  }

  /**
    * Completes one promise after another and collects all callbacks from them.
    * Already completed promises or promises without callbacks are ignored.
    * @param promises
    * @param v
    * @return The parent callback entry which links all callbacks from all completed promises together.
    */
  private def tryCompleteAllAndGetCallbacks(promises: Set[Self], v: Try[T]): Option[CallbackEntry] = {
    val flattened = promises.map(p => p.tryCompleteAndGetCallback(v)).flatten
    if (flattened.isEmpty) { None } else {
      Some(flattened.reduce((c0, c1) => ParentCallbackEntry(c0, c1)))
    }
  }

  /**
    * Tries to complete the promise and returns its callback entry.
    * If it is already completed or has no callback entry/[[Noop]], the result is `None`.
    * @param v
    * @return
    */
  @inline @tailrec private def tryCompleteAndGetCallback(v: Try[T]): Option[CallbackEntry] = {
    val s = get
    s match {
      case FixedValueTypeTry(_) => None
      case FixedValueTypeCallbackEntry(x) =>
        if (compareAndSet(s, FixedValueTypeTry(v))) {
          if (x ne Noop) {
            Some(x)
          } else { None }
        } else { tryCompleteAndGetCallback(v) }
      case FixedValueTypeLink(links, c) => {
        if (compareAndSet(s, FixedValueTypeTry(v))) {
          val targetCallbacks = tryCompleteAllAndGetCallbacks(links, v)

          if (targetCallbacks.isDefined) {
            Some(ParentCallbackEntry(c, targetCallbacks.get))
          } else {
            Some(c)
          }
        } else {
          tryCompleteAndGetCallback(v)
        }
      }
    }
  }

  @inline @tailrec private def onCompleteInternal(c: Callback): Unit = {
    val s = get
    s match {
      case FixedValueTypeTry(x)           => dispatchCallback(x, c)
      case FixedValueTypeCallbackEntry(x) => if (!compareAndSet(s, FixedValueTypeCallbackEntry(appendCallback(x, c)))) onCompleteInternal(c)
      // Just replace the callback entry in the current link. Do not move any callbacks to target promises.
      case FixedValueTypeLink(links, callbackEntry) =>
        if (!compareAndSet(s, FixedValueTypeLink(links, LinkedCallbackEntry(c, callbackEntry)))) onCompleteInternal(c)
    }
  }

  @inline @tailrec private def tryCompleteWithInternal(other: FP[T]): Unit = {
    if (other.isInstanceOf[Self]) {
      val o = other.asInstanceOf[Self]
      val s = o.get
      s match {
        case FixedValueTypeTry(x) => tryComplete(x)
        case FixedValueTypeCallbackEntry(x) => {
          // Replace the callback list by a link to this which still holds the callback.
          if (!o.compareAndSet(s, FixedValueTypeLink(Set(this), x))) tryCompleteWithInternal(other)
        }
        // Add this as additional target for the link.
        case FixedValueTypeLink(links, c) => if (!o.compareAndSet(s, FixedValueTypeLink[T](links + this, c))) tryCompleteWithInternal(other)
      }
    } else {
      other.onComplete(this.tryComplete(_))
    }
  }

  /**
    * The following methods exist for tests only.
    * @param primCASPromiseLinking The target promise which this should be a direct link to.
    * @return True if this is a direct link to the target promise. Otherwise, false.
    */
  private[cas] def isLinkTo(primCASPromiseLinking: Self): Boolean = {
    val s = get
    s match {
      case FixedValueTypeTry(_) | FixedValueTypeCallbackEntry(_) => false
      case FixedValueTypeLink(links, _)                          => links.contains(primCASPromiseLinking)
    }
  }

  private[cas] def isLink(): Boolean = {
    val s = get
    s match {
      case FixedValueTypeTry(_) | FixedValueTypeCallbackEntry(_) => false
      case FixedValueTypeLink(_, _)                              => true
    }
  }

  private[cas] def getLinkTo(): Set[Self] = {
    val s = get
    s match {
      case FixedValueTypeTry(_) | FixedValueTypeCallbackEntry(_) => throw new RuntimeException("Invalid usage.")
      case FixedValueTypeLink(links, _)                          => links
    }
  }

  private[cas] def isListOfCallbacks(): Boolean = {
    val s = get
    s match {
      case FixedValueTypeTry(_) | FixedValueTypeLink(_, _) => false
      case FixedValueTypeCallbackEntry(_)                  => true
    }
  }

  private[cas] def getNumberOfCallbacks(): Int = {
    val s = get
    s match {
      case FixedValueTypeTry(_)           => throw new RuntimeException("Is not a list of callbacks.")
      case FixedValueTypeCallbackEntry(x) => getNumberOfCallbacks(x)
      case FixedValueTypeLink(_, x)       => getNumberOfCallbacks(x)
    }
  }

  private def getNumberOfCallbacks(c: CallbackEntry): Int = c match {
    case SingleCallbackEntry(_)           => 1
    case ParentCallbackEntry(left, right) => getNumberOfCallbacks(left) + getNumberOfCallbacks(right)
    case Noop                             => 0
    case LinkedCallbackEntry(_, prev)     => 1 + getNumberOfCallbacks(prev)
  }
}
