package tdauth.futuresandpromises.twitter

import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.UsingUninitializedTry

class TwitterTry[T](val o: scala.Option[com.twitter.util.Try[T]]) extends Try[T] {

  def this() = this(None)

  def this(t: com.twitter.util.Try[T]) = this(Some(t))

  override def get(): T = {
    o match {
      case Some(t) => t.get
      case None => throw new UsingUninitializedTry
    }
  }

  override def hasException: Boolean = {
    o match {
      case Some(t) => t.isThrow
      case None => false
    }

  }

  override def hasValue: Boolean = {
    o match {
      case Some(t) => t.isReturn
      case None => false
    }
  }
}