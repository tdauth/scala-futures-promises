package tdauth.futuresandpromises.combinators

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.standardlibrary.ScalaFPFuture

class CombinatorsFuture[T](f: scala.concurrent.Future[T]) extends ScalaFPFuture[T](f) {
  /**
   * Uses the combinator {@link #orElse} instead of a promise-based implementation.
   *
   * To rethrow the correct exception we need to know the failing order of the futures because the final future should fail with the final failed future if both futures fail.
   * @todo Simplify the implementation of throwing the final exception if possible.
   */
  def firstSuccWithOrElse(other: Future[T]): Future[T] = {
    /*
     * Stores the order of the failed exceptions for resolving it in case both futures fail.
     */
    val ctx = scala.collection.mutable.ArrayBuffer.empty[Int]
    val f0 = this.then((t: Try[T]) => {
      if (t.hasException) {
        ctx.synchronized {
          ctx += 0
        }
      }

      t.get()
    }).orElse(other)
    val f1 = other.then((t: Try[T]) => {
      if (t.hasException) {
        ctx.synchronized {
          ctx += 1
        }
      }

      t.get()
    }).orElse(this)

    f0.first(f1).then((t: Try[T]) => {
      if (t.hasException) {
        if (ctx(0) == 0) {
          other.get
        } else {
          this.get
        }
      }

      t.get
    })
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