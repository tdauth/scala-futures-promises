package main.scala.futuresandpromises

import scala.collection.immutable.Vector

trait Util {
  def async[Func, T](ex: Executor, f: Func): Future[T]

  // Derived methods:
  def firstN[T](c: Vector[Future[T]], n: Integer): Future[Vector[Tuple2[Integer, Future[T]]]]
  def firstNSucc[T](c: Vector[Future[T]], n: Integer): Future[Vector[Tuple2[Integer, T]]]
}