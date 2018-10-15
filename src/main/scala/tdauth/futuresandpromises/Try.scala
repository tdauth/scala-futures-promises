package tdauth.futuresandpromises

import scala.util.Failure
import scala.util.Success
import scala.util.control.NonFatal

/**
 * Can either hold a successful result value or an exception.
 * This is similiar to [[https://www.scala-lang.org/api/current/scala/util/Try.html Scala FP's Try] and
 * [[https://twitter.github.io/util/docs/com/twitter/util/Try.html Twitter's Try]].
 * In Folly it can be empty, too: [[https://github.com/facebook/folly/blob/master/folly/Try.h Try]] but this is not
 * possible in Scala.
 * This is because it is directly used as the result value of a future in Folly instead of nesting it in an optional value.
 * See the field `result_` in Folly's [[https://raw.githubusercontent.com/facebook/folly/master/folly/futures/detail/Core.h Core.h]].
 */
class Try[T](t: scala.util.Try[T]) {

  // Basic methods:
  def this(v: T) = this(Success(v))

  def this(e: Throwable) = this(Failure(e))

  /**
   * Gets the currently hold value of the Try.
   * @return Returns the currently hold value of the Try. If it has an exception, the exception is rethrown.
   */
  def get(): T = t.get

  def hasException: Boolean = t.isFailure

  def hasValue: Boolean = t.isSuccess

  // Derived methods:
  def getException: Option[Throwable] = {
    try {
      get
      None
    } catch {
      case NonFatal(x) => Some(x)
    }
  }
}