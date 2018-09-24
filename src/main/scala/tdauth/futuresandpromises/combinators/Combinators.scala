package tdauth.futuresandpromises.combinators

import scala.util.control.NonFatal

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.standardlibrary.ScalaFPPromise
import tdauth.futuresandpromises.Util
import tdauth.futuresandpromises.standardlibrary.ScalaFPUtil

/**
 * Additional implementations of the non-blocking combinators.
 */
object Combinators {

  /**
   * Uses the combinator {@link Future#orElse} instead of a promise-based implementation.
   *
   * We need to know the failing order of the futures to rethrow the correct exception since the resulting future should fail with
   * the final failed future if both futures have failed.
   * This is the same behaviour as in {@link Future#firstSucc}.
   * TODO Simplify the implementation of throwing the final exception if possible.
   */
  def firstSuccWithOrElse[T](t: Future[T], other: Future[T]): Future[T] = {
    /*
     * Stores the exception of the first or the second future for resolving the order in case both futures fail.
     */
    class CustomException(var cause: Throwable = null, var counter: Int = 0)
    val ctx = new CustomException
    val callback = (t: Try[T]) => {
      try {
        t.get()
      } catch {
        case NonFatal(x) => {
          ctx.synchronized {
            ctx.cause = x
            ctx.counter += 1
          }

          throw x
        }
      }
    }
    val f0 = t.then(callback).orElse(other)
    val f1 = other.then(callback).orElse(t)

    /*
     * Make sure to rethrow the final exception.
     * Does not need synchronized since it will be the only access of ctx.
     */
    f0.first(f1).then((t: Try[T]) => if (ctx.counter == 2) throw ctx.cause else t.get)
  }

  /**
   * Uses {@link Util#firstN} instead of a promise-based implementation.
   */
  def firstWithFirstN[T](t: Future[T], other: Future[T]): Future[T] = {
    ScalaFPUtil.firstN[T](Vector(t, other), 1).then((t: Try[Util#FirstNResultType[T]]) => {
      t.get()(0)._2.get()
    })
  }

  /**
   * Uses {@link Util#firstNSucc} instead of a promise-based implementation.
   */
  def firstSuccWithFirstNSucc[T](t: Future[T], other: Future[T]): Future[T] = {
    ScalaFPUtil.firstNSucc[T](Vector(t, other), 1).then((t: Try[Util#FirstNSuccResultType[T]]) => {
      t.get()(0)._2
    })
  }

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
   * @param c The input futures which can still be completed.
   * @param n The number of the remaining futures which have to be completed.
   * @param resultVector The current result vector with all already completed results.
   * @param indexMap Stores the current indices as keys and the original indices as values. This map is required for accessing the original index of a future when its Try instance is added to the result.
   */
  private def firstNWithFirstInternal[T](c: Vector[Future[T]], n: Integer, resultVector: Util#FirstNResultType[T], indexMap: Map[Int, Int]): Future[Util#FirstNResultType[T]] = {
    if (c.size < n) {
      val p = new ScalaFPPromise[Util#FirstNResultType[T]]()
      p.tryFailure(new RuntimeException("Not enough futures"))

      p.future()
    } else {
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
  }

  // TODO Implement firstSucc in the same way.

  /**
   * Possible new combinators for futures.
   * These combinators are inspired by the Scala standard library API: https://www.scala-lang.org/api/2.12.3/scala/collection/GenTraversableOnce.html
   */
  /**
   * There is several attempts to allow retries of futures until they succeed or a user-defined condition is satisfied:
   * <ul>
   * <li>http://www.home.hs-karlsruhe.de/~suma0002/publications/retriable-futures.pdf</li>
   * <li>https://github.com/softwaremill/retry</li>
   * <li>https://issues.scala-lang.org/browse/SI-8615</li>
   * <li>https://stackoverflow.com/questions/7930814/whats-the-scala-way-to-implement-a-retry-able-call-like-this-one</li>
   * <li>https://github.com/facebook/folly/blob/master/folly/futures/Retrying.h</li>
   * <li>https://github.com/facebook/folly/blob/master/folly/futures/test/RetryingTest.cpp</li>
   * <li>https://hackernoon.com/exponential-back-off-with-scala-futures-7426340d0069</li>
   * </ul>
   * Some of them allow backing off strategies to increase the time intervals before retrying again.
   * This reduces unnecessary computing power on useless retries.
   */
  //def retry[T](f : Future[T], p: T => Boolean) : Future[T]
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
}