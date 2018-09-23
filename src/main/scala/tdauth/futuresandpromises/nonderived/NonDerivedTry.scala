package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.UsingUninitializedTry
import tdauth.futuresandpromises.standardlibrary.ScalaFPTry
import tdauth.futuresandpromises.standardlibrary.ScalaFPTry

class NonDerivedTry[T](o: scala.Option[scala.util.Try[T]]) extends ScalaFPTry[T](o) {

  def this() = this(None)

  def this(t: scala.util.Try[T]) = this(Some(t))
}