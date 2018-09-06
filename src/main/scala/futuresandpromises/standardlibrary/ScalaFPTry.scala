package main.scala.futuresandpromises.standardlibrary

import main.scala.futuresandpromises.Try

class ScalaFPTry[T](val t: scala.util.Try[T]) extends Try[T] {
  override def get(): T = t.get
  override def hasException: Boolean = t.isFailure
  override def hasValue: Boolean = t.isSuccess
}