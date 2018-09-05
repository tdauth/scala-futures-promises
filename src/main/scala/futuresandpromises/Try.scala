package main.scala.futuresandpromises

trait Try[T] {
  // TODO Define abstract constructors:
  //abstract def this(v : T)
  //abstract def this(e : Exception)
  /**
   * @throws IsEmptyException If there is neither a result value nor an exception, this exception will be thrown.
   */
  def get(): T
  def hasValue: Boolean
  def hasException: Boolean
}