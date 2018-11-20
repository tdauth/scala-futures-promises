package tdauth.futuresandpromises.core.cas

import tdauth.futuresandpromises._

import scala.annotation.tailrec
import scala.util.Left

/**
  * The same as [[CCAS]] but dues only submit one callback after another to the executor when the future is completed.
  */
class CCASOneCallbackAtATime[T](ex: Executor) extends CCAS[T](ex) {

  override def tryCompleteC(v: Try[T]): Boolean = tryCompleteInternal(v)

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
