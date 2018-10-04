package tdauth.futuresandpromises

import scala.util.Failure
import scala.util.Success

/**
 * Can either be empty, hold a success value or an exception.
 * This is different from Scala's [[https://www.scala-lang.org/api/current/scala/util/Try.html scala.util.Try]] which cannot be empty
 *  and more like Folly's [[https://github.com/facebook/folly/blob/master/folly/Try.h Try]].
 */
class Try[T](o: scala.Option[scala.util.Try[T]]) {

  def this() = this(None)

  def this(t: scala.util.Try[T]) = this(Some(t))

  def this(v : T) = this(Some(Success(v)))

  def this(e : Throwable) = this(Some(Failure(e)))

  /**
   * Gets the currently hold value of the Try.
   * @return Returns the currently hold value of the Try. If it has an exception, the exception is rethrown.
   * @throws UsingUninitializedTry If there is neither a result value nor an exception, this exception will be thrown.
   */
  def get(): T = {
    o match {
      case Some(t) => t.get
      case None => throw new UsingUninitializedTry
    }
  }

  def hasException: Boolean = {
    o match {
      case Some(t) => t.isFailure
      case None => false
    }

  }

  def hasValue: Boolean = {
    o match {
      case Some(t) => t.isSuccess
      case None => false
    }
  }
}