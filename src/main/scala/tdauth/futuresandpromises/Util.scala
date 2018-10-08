package tdauth.futuresandpromises

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.immutable.Vector
import scala.util.control.NonFatal

trait Util {
  def factory: Factory

  // Derived methods:
  def async[T](ex: Executor, f: () => T): Future[T] = {
    val p = factory.createPromise[T](ex)

    ex.submit(() => {
      try {
        p.trySuccess(f.apply())
      } catch {
        case NonFatal(e) => p.tryFailure(e)
      }
    })

    p.future()
  }

  type FirstNResultType[T] = Vector[Tuple2[Int, Try[T]]]

  def firstN[T](ex: Executor, futures: Vector[Future[T]], n: Integer): Future[FirstNResultType[T]] = {
    class FirstNContext {
      var v: FirstNResultType[T] = Vector()
      val completed = new AtomicInteger(0)
      val p = factory.createPromise[FirstNResultType[T]](ex)
    }

    val ctx = new FirstNContext

    if (futures.size < n) {
      ctx.p.tryFailure(new RuntimeException("Not enough futures"))
    } else {
      for ((f, i) <- futures.view.zipWithIndex) {
        f.onComplete((t: Try[T]) => {
          val completed = ctx.completed.incrementAndGet

          if (completed <= n) {
            var vectorSize = -1

            ctx.synchronized {
              ctx.v = ctx.v :+ (i, t)
              vectorSize = ctx.v.length
            }

            /*
             * Compare to the actual vector size after the atomic insert operation, to prevent possible data races.
             * Otherwise, if compared to "completed", it could complete the promise before the other insert operations are done.
             */
            if (vectorSize == n) {
              ctx.p.trySuccess(ctx.v)
            }
          }
        })
      }
    }

    ctx.p.future
  }

  type FirstNSuccResultType[T] = Vector[Tuple2[Int, T]]

  def firstNSucc[T](ex: Executor, futures: Vector[Future[T]], n: Integer): Future[FirstNSuccResultType[T]] = {
    class FirstNSuccContext {
      var v: FirstNSuccResultType[T] = Vector()
      val succeeded = new AtomicInteger(0)
      val failed = new AtomicInteger(0)
      val p = factory.createPromise[FirstNSuccResultType[T]](ex)
    }

    val ctx = new FirstNSuccContext
    val total = futures.size

    if (total < n) {
      ctx.p.tryFailure(new RuntimeException("Not enough futures"))
    } else {
      for ((f, i) <- futures.view.zipWithIndex) {
        f.onComplete((t: Try[T]) => {

          /*
           * Ignore exceptions until as many futures failed that n futures cannot be completed successfully anymore.
           * Even if a future fails after the promise has been completed and leads to completing the promise with a failure, the promise can
           * not be completed any more (write-once semantics) and the tryFailure call simply fails.
           * Therefore, we do not need any atomic "done" flag.
           */
          if (t.hasException) {
            val completed = ctx.failed.incrementAndGet

            /*
             * Since the local variable can never have the counter incremented by more than one,
             * we can check for the exact final value and do only one setException call.
             */
            if (total - completed + 1 == n) {
              try {
                t.get
              } catch {
                case NonFatal(e) => ctx.p.tryFailure(e)
              }
            }
          } else {
            val completed = ctx.succeeded.incrementAndGet

            if (completed <= n) {
              var vectorSize = -1

              ctx.synchronized {
                ctx.v = ctx.v :+ (i, t.get)
                vectorSize = ctx.v.length
              }

              /*
               * Compare to the actual vector size after the atomic insert operation, to prevent possible data races.
               * Otherwise, if compared to "completed", it could complete the promise before the other insert operations are done.
               */
              if (vectorSize == n) {
                ctx.p.trySuccess(ctx.v)
              }
            }
          }
        })
      }
    }

    ctx.p.future
  }
}