package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.UsingUninitializedTry

class StmTry[T](val o: scala.Option[scala.util.Try[T]]) extends Try[T] {

  def this() = this(None)

  def this(t: scala.util.Try[T]) = this(Some(t))

  override def get(): T = {
    o match {
      case Some(t) => t.get
      case None => throw new UsingUninitializedTry
    }
  }

  override def hasException: Boolean = {
    o match {
      case Some(t) => t.isFailure
      case None => false
    }
  }

  override def hasValue: Boolean = {
    o match {
      case Some(t) => t.isSuccess
      case None => false
    }
  }
}