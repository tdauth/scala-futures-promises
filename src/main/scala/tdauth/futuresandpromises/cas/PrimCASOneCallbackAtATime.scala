package tdauth.futuresandpromises.cas
import tdauth.futuresandpromises._

import scala.annotation.tailrec
import scala.util.Left

/**
  * The same as [[PrimCAS]] but dues only submit one callback after another to the executor when the future is completed.
  */
class PrimCASOneCallbackAtATime[T](ex: Executor) extends PrimCAS[T](ex) {

  override def tryComplete(v: Try[T]): Boolean = tryCompleteInternal(v)

  @tailrec private def tryCompleteInternal(v: Try[T]): Boolean = {
    val s = get
    s match {
      case Left(x) => false
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
}
