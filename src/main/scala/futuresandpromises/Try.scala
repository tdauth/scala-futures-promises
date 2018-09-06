package main.scala.futuresandpromises

trait Try[T] {
  /**
   * @throws UsingUninitializedTry If there is neither a result value nor an exception, this exception will be thrown.
   */
  def get(): T
  def hasValue: Boolean
  def hasException: Boolean
}