package tdauth.futuresandpromises

/**
 * Can either be empty, hold a success value or an exception.
 * This is different from Scala's [[https://www.scala-lang.org/api/current/scala/util/Try.html scala.util.Try]] and more
 * like Folly's [[https://github.com/facebook/folly/blob/master/folly/Try.h Try]].
 */
trait Try[T] {
  /**
   * Gets the currently hold value of the Try.
   * @return Returns the currently hold value of the Try. If it has an exception, the exception is rethrown.
   * @throws UsingUninitializedTry If there is neither a result value nor an exception, this exception will be thrown.
   */
  def get(): T
  def hasValue: Boolean
  def hasException: Boolean
}