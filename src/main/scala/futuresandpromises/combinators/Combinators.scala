package tdauth.futuresandpromises.combinators

import scala.util.control.NonFatal

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.standardlibrary.ScalaFPPromise
import tdauth.futuresandpromises.Util

/**
 * Additional implementations of the non-blocking combinators.
 */
object Combinators {

  /**
   * Uses the combinator {@link Future#orElse} instead of a promise-based implementation.
   *
   * We need to know the failing order of the futures to rethrow the correct exception since the resulting future should fail with
   * the final failed future if both futures have failed.
   * This is the same behaviour as in {@link Future#orElse}.
   * TODO Simplify the implementation of throwing the final exception if possible.
   */
  def firstSuccWithOrElse[T](t: Future[T], other: Future[T]): Future[T] = {
    /*
     * Stores if the first or the second future has failed finally for resolving the order in case both futures fail.
     */
    final case class CustomException(var cause: Throwable = None.orNull)
    val ctx = CustomException(null)
    val callback = (t: Try[T]) => {
      try {
        t.get()
      } catch {
        case NonFatal(x) => {
          ctx.synchronized {
            ctx.cause = x
          }

          throw x
        }
      }
    }
    val f0 = t.then(callback).orElse(other)
    val f1 = other.then(callback).orElse(t)

    f0.first(f1).then((t: Try[T]) => {
      /*
       * Make sure to rethrow the final exception.
       */
      if (t.hasException) {
        throw ctx.cause
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

  /**
   * The implementation is based on the following code:
   * <pre>
   * ((f0.first(f1)).first(f2)) ... first(n)
   * </pre>
   * This is done n times. Each time the successful future is removed from the input vector, so only the left futures can be added.
   * Therefore, the original indices have to be stored in a map and reproduced whenever a Try value is added to the resulting vector.
   *
   * Except for the error checking in the beginning, this implementation does not require the use of promises, unline {@link Util#firstN}.
   */
  def firstNWithFirst[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNResultType[T]] = firstNWithFirstInternal[T](c, n, Vector(), (0 to c.size).map { i => (i, i) }.toMap)

  /**
   * @param indexMap Stores the current indices as keys and the original indices as values. This map is required for accessing the original index of a future when its Try instance is added to the result.
   */
  private def firstNWithFirstInternal[T](c: Vector[Future[T]], n: Integer, resultVector: Util#FirstNResultType[T], indexMap: Map[Int, Int]): Future[Util#FirstNResultType[T]] = {
    if (c.size < n) {
      val p = new ScalaFPPromise[Util#FirstNResultType[T]]()
      p.tryFailure(new RuntimeException("Not enough futures"))

      return p.future()
    }

    var result = c(0).then((t: Try[T]) => (0, t))

    1 to c.size - 1 foreach { i => result = result.first(c(i).then((t: Try[T]) => (i, t))) }

    result.then((t: Try[Tuple2[Int, Try[T]]]) => {
      val r = t.get()
      val index = r._1
      val realIndex = indexMap(index)
      val newResultVector = resultVector :+ (realIndex, r._2)

      if (n > 1) {
        // Remove the element with the given index, so it cannot be added to the completed futures anymore.
        val newC = c.patch(index, Nil, 1)
        /*
         * Remove the current index from the indexMap since the element cannot be used for the result anymore.
         * All current indices which are bigger than the removed index must by decreased to stay valid.
         * Indices which are smaller than the removed index can stay as they are.
         */
        val newIndexMap = (indexMap - index).map {
          case (key, value) => {
            if (key > index) {
              (key - 1, value)
            } else {
              (key, value)
            }
          }
        }
        /*
         * Call this method recursively to add the remaining n futures.
         */
        firstNWithFirstInternal[T](newC, n - 1, newResultVector, newIndexMap).get
      } else {
        newResultVector
      }
    })
  }

  // TODO Implement firstSucc in the same way.
}