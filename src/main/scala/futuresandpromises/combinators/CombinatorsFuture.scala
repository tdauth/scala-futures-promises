package tdauth.futuresandpromises.combinators

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.standardlibrary.ScalaFPFuture

class CombinatorsFuture[T](f: scala.concurrent.Future[T]) extends ScalaFPFuture[T](f) {
  def firstSuccWithOrElse(other: Future[T]): Future[T] = {
    val f0 = this.orElse(other)
    val f1 = other.orElse(this)

    /*
     * If f0 fails it waits for f1 and if f1 fails, f0 will fail with the exception of this and f1 will fail with the exception of other.
     * Probably, f0 will win (since it is the first parameter and therefore when both fail, it will always fail with the exception of f0.
     * However, our current firstSucc implementation would wait forever.
     */
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