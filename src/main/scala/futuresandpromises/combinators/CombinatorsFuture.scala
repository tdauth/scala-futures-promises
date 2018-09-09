package tdauth.futuresandpromises.combinators

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.standardlibrary.ScalaFPFuture

class CombinatorsFuture[T](f: scala.concurrent.Future[T]) extends ScalaFPFuture[T](f) {
  /**
   * Uses the combinator {@link #orElse} instead of a promise-based implementation.
   */
  def firstSuccWithOrElse(other: Future[T]): Future[T] = {
    val f0 = this.orElse(other).then((t: Try[T]) => {
      /*
       * Make sure that it fails with the second exception if it failed to prevent using the first exception since we want to use the final exception.
       */
      if (t.hasException) {
        other.get // rethrows the exception of other
      }

      t.get
    })
    val f1 = other.orElse(this).then((t: Try[T]) => {
      /*
       * Make sure that it fails with the second exception if it failed to prevent using the first exception since we want to use the final exception.
       */
      if (t.hasException) {
        this.get // rethrows the exception of this
      }

      t.get
    })

    f0.first(f1)
  }

  /**
   * Possible new combinators for futures.
   * These combinators are inspired by the Scala standard library API: https://www.scala-lang.org/api/2.12.3/scala/collection/GenTraversableOnce.html
   * @{
   */
  /**
   * The current method {@link Future#first} prioritizes the first future if both futures have already been completed.
   * When both futures complete at the exact same time, it depends which callback is called first and therefore which {@link Promise#tryComplete} call is executed first.
   * This implementation is similar to {@link Future#first} but should be like https://golang.org/ref/spec#Select_statements and select the first future randomly if both have been completed.
   */
  //def select(other: Future[T]): Future[T]
  /**
   * If both futures have the same value, the resulting future will be completed with true.
   * Otherwise, it will be completed with false.
   */
  //def equals(other : Future[T]) : Future[Boolean]
  //def map(other : Future[T], f : (T) => T) : Future[T]
  /**
   * https://stackoverflow.com/questions/7764197/difference-between-foldleft-and-reduceleft-in-scala
   */
  //def fold[B](z: Future[B])(f: (B, T) => B): Future[B]
  //def foldLeft[B](z: Future[B])(f: (B, T) => B): Future[B]
  //def foldRight[B](z: Future[B])(f: (B, T) => B): Future[B]
  //def reduce[B >: T] (f: (B, T) => B): Future[B]
  //def reduceLeft[B >: T] (f: (B, T) => B): Future[B]
  //def reduceRight[B >: T] (f: (B, T) => B): Future[B]
  /**
   * }@
   */
}