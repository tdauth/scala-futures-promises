package main.scala.futuresandpromises

import scala.collection.immutable.Vector

trait Util {
  def async[T](ex: Executor, f: () => T): Future[T]

  // Derived methods:
  def firstN[T](c: Vector[Future[T]], n: Integer): Future[Vector[Tuple2[Integer, Future[T]]]] = {
		// TODO Implement. Maybe Future.firstCompletedOf can help.
  }

  def firstNSucc[T](c: Vector[Future[T]], n: Integer): Future[Vector[Tuple2[Integer, T]]] = {
		// TODO Implement.
  }
}