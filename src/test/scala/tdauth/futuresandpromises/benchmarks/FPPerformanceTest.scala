package tdauth.futuresandpromises

import java.util.concurrent.Executors

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Success

import org.scalameter.api.Bench
import org.scalameter.api.Gen

import com.twitter.util.Return

import tdauth.futuresandpromises.cas.PrimCAS
import tdauth.futuresandpromises.mvar.PrimMVar
import tdauth.futuresandpromises.stm.PrimSTM
import java.util.concurrent.ExecutorService

/**
 * Compares the performance of our FP implementation to the performance of Scala FP and Twitter Util.
 *
 * Two reasons why Scala FP newP is much faster:
 * - Optimization with Noop for really empty promises
 * - extends AtomicReference instead of having it as a field. We can do the same but we have to rename `get`
 * TODO Find out where we get the standard constructor from Twitter Util.
 * Overall the total time of newP (although 100000) is not bigger than 90 ms.
 *
 * Even tryComplete is not more than 190 ms.
 *
 * TODO #29 Twitter util onComplete is much faster, probably because there is no executor.
 *
 * TODO #29 Exclude the executor construction and shutdown from the test, or do it at least outside.
 *
 * TODO #29 Add stress test which combines followedBy for n futures and then calls `first`.
 *
 * TODO #29 Add using(NUMBER_OF_THREADS) to increase the number of threads. There is no possibility to limit the testing JVM to some CPU cores?
 * Limiting the executor threads should have the same effect.
 */
object FPPerformanceTest extends Bench.OfflineReport {
  val NUMBER_OF_NEW_PS = Gen.range("size")(10000, 100000, 10000)
  val NUMBER_OF_TRY_COMPLETE = Gen.range("size")(10000, 100000, 10000)
  val NUMBER_OF_ON_COMPLETES = Gen.range("size")(100, 1000, 100) // 1000, 10000, 1000
  val NUMBER_OF_FOLLOWED_BYS = Gen.range("size")(100, 1000, 100)
  val NUMBER_OF_FOLLOWED_BY_WITHS = Gen.range("size")(1000, 10000, 1000)

  // TODO #29 Do we even need executors and if so should we scale the number of threads?
  val executionService = Executors.newSingleThreadExecutor()
  // TODO #29 Executor for Twitter?
  // See https://groups.google.com/forum/#!topic/finaglers/ovDL2UFKoDw
  val twitterExecutor = com.twitter.util.FuturePool.apply(executionService)

  def getScalaFPExecutor(n: Int): Tuple2[ExecutorService, ExecutionContext] = {
    val executionService = Executors.newFixedThreadPool(n)
    (executionService, ExecutionContext.fromExecutorService(executionService))
  }

  def getPrimExecutor(n: Int): Executor = {
    val executionService = Executors.newFixedThreadPool(n)
    new JavaExecutor(executionService)
  }

  performance of "Twitter Util" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield com.twitter.util.Promise.apply[Int]
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield com.twitter.util.Promise.apply[Int]
            promises.foreach(p => p.updateIfEmpty(Return(r)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = com.twitter.util.Promise.apply[Int]
            registerOnCompletesTwitter(p, r)
            p.updateIfEmpty(Return(r))
            com.twitter.util.Await.ready(p)
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            val p = com.twitter.util.Promise.apply[Int]
            val nested = nestedFollowedByTwitter(p, r)
            p.updateIfEmpty(Return(r))
            com.twitter.util.Await.ready(p)
          }
      }
    }

    measure method "followedByWith" in {
      using(NUMBER_OF_FOLLOWED_BY_WITHS) in {
        r =>
          {
            val p = com.twitter.util.Promise.apply[Int]
            val nested = nestedFollowedByWithTwitter(p, r)
            p.updateIfEmpty(Return(r))
            com.twitter.util.Await.ready(nested)
          }
      }
    }
  }

  performance of "Scala FP" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield scala.concurrent.Promise.apply[Int]
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield scala.concurrent.Promise.apply[Int]
            promises.foreach(p => p.tryComplete(Success(r)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            // TODO Set executor only once as implicit value and don't pass it recursively?
            val ex = getScalaFPExecutor(1)
            val p = scala.concurrent.Promise.apply[Int]
            val f = p.future
            registerOnCompletesScalaFP(f, r, ex._2)
            p.trySuccess(10)
            Await.ready(f, Duration.Inf)
            ex._1.shutdownNow
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            // TODO Set executor only once as implicit value and don't pass it recursively?
            val ex = getScalaFPExecutor(1)
            val p = scala.concurrent.Promise.apply[Int]
            val f = p.future
            val nested = nestedMap(f, r, ex._2)
            p.trySuccess(r)
            Await.ready(f, Duration.Inf)
            ex._1.shutdownNow
          }
      }
    }

    measure method "followedByWith" in {
      using(NUMBER_OF_FOLLOWED_BY_WITHS) in {
        r =>
          {
            val ex = getScalaFPExecutor(1)
            val p = scala.concurrent.Promise.apply[Int]
            val nested = nestedFollowedByWithScalaFP(p.future, r, ex._2)
            p.trySuccess(r)
            Await.ready(nested, Duration.Inf)
            ex._1.shutdownNow
          }
      }
    }
  }

  performance of "PrimCAS" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield new PrimCAS[Int](null)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimCAS[Int](null)
            promises.foreach(p => p.tryComplete(new Try[Int](r)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimCAS[Int](ex)
            registerOnCompletes(p, r)
            p.trySuccess(r)
            p.get
            ex.shutdown
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimCAS[Int](ex)
            val nested = nestedFollowedBy(p, r)
            p.trySuccess(r)
            nested.get
            ex.shutdown
          }
      }
    }

    measure method "followedByWith" in {
      using(NUMBER_OF_FOLLOWED_BY_WITHS) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimCAS[Int](ex)
            val nested = nestedFollowedByWithsCas(p, r, ex)
            p.trySuccess(r)
            nested.get
            ex.shutdown
          }
      }
    }
  }

  performance of "PrimMVar" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield new PrimMVar[Int](null)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimMVar[Int](null)
            promises.foreach(p => p.tryComplete(new Try[Int](r)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimMVar[Int](ex)
            registerOnCompletes(p, r)
            p.trySuccess(r)
            p.get
            ex.shutdown
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimMVar[Int](ex)
            val nested = nestedFollowedBy(p, r)
            p.trySuccess(r)
            nested.get
            ex.shutdown
          }
      }
    }

    measure method "followedByWith" in {
      using(NUMBER_OF_FOLLOWED_BY_WITHS) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimMVar[Int](ex)
            val nested = nestedFollowedByWithsMVar(p, r, ex)
            p.trySuccess(r)
            nested.get
            ex.shutdown
          }
      }
    }
  }

  performance of "PrimSTM" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield new PrimSTM[Int](null)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimSTM[Int](null)
            promises.foreach(p => p.tryComplete(new Try[Int](r)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimSTM[Int](ex)
            registerOnCompletes(p, r)
            p.trySuccess(r)
            p.get
            ex.shutdown
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimSTM[Int](ex)
            val nested = nestedFollowedBy(p, r)
            p.trySuccess(r)
            nested.get
            ex.shutdown
          }
      }
    }

    measure method "followedByWith" in {
      using(NUMBER_OF_FOLLOWED_BY_WITHS) in {
        r =>
          {
            val ex = getPrimExecutor(1)
            val p = new PrimSTM[Int](ex)
            val nested = nestedFollowedByWithsStm(p, r, ex)
            p.trySuccess(r)
            nested.get
            ex.shutdown
          }
      }
    }
  }

  // Twiter Util methods:
  // TODO #29 Use twitterExecutor
  @tailrec def registerOnCompletesTwitter(f: com.twitter.util.Future[Int], i: Int) {
    f.respond(t => t.get())
    if (i > 1) registerOnCompletesTwitter(f, i - 1)
  }

  // TODO #29 Use twitterExecutor
  @tailrec def nestedFollowedByTwitter(f: com.twitter.util.Future[Int], i: Int): com.twitter.util.Future[Int] = {
    val p = f.map(t => t + 1)
    if (i > 1) nestedFollowedByTwitter(p, i - 1) else p
  }

  @tailrec def nestedFollowedByWithTwitter(f: com.twitter.util.Future[Int], i: Int): com.twitter.util.Future[Int] = {
    val successful = com.twitter.util.Promise[Int]
    successful.setValue(i)
    val p = successful.flatMap(t => f)
    if (i > 1) nestedFollowedByWithTwitter(p, i - 1) else p
  }

  // Scala FP methods:
  @tailrec def registerOnCompletesScalaFP(f: scala.concurrent.Future[Int], i: Int, ex: ExecutionContext) {
    f.onComplete(t => t.get)(ex)
    if (i > 1) registerOnCompletesScalaFP(f, i - 1, ex)
  }

  @tailrec def nestedMap(f: scala.concurrent.Future[Int], i: Int, ex: ExecutionContext): scala.concurrent.Future[Int] = {
    val p = f.map(t => t + 1)(ex)
    if (i > 1) nestedMap(p, i - 1, ex) else p
  }

  @tailrec def nestedFollowedByWithScalaFP(f: scala.concurrent.Future[Int], i: Int, ex: ExecutionContext): scala.concurrent.Future[Int] = {
    val p = scala.concurrent.Promise.successful(i).future.flatMap(t => f)(ex)
    if (i > 1) nestedFollowedByWithScalaFP(p, i - 1, ex) else p
  }

  // Prim methods:
  @tailrec def registerOnCompletes(f: FP[Int], i: Int) {
    f.onComplete(t => t.get())
    if (i > 1) registerOnCompletes(f, i - 1)
  }

  @tailrec def nestedFollowedBy(f: FP[Int], i: Int): FP[Int] = {
    val p = f.followedBy(t => t + 1)
    if (i > 1) nestedFollowedBy(p, i - 1) else p
  }

  @tailrec def nestedFollowedByWithsCas(f: FP[Int], i: Int, ex: Executor): FP[Int] = {
    val successfulP = new PrimCAS[Int](ex)
    successfulP.trySuccess(i)
    val p = successfulP.followedByWith(t => f)
    if (i > 1) nestedFollowedByWithsCas(p, i - 1, ex) else p
  }

  @tailrec def nestedFollowedByWithsMVar(f: FP[Int], i: Int, ex: Executor): FP[Int] = {
    val successfulP = new PrimMVar[Int](ex)
    successfulP.trySuccess(i)
    val p = successfulP.followedByWith(t => f)
    if (i > 1) nestedFollowedByWithsMVar(p, i - 1, ex) else p
  }

  @tailrec def nestedFollowedByWithsStm(f: FP[Int], i: Int, ex: Executor): FP[Int] = {
    val successfulP = new PrimSTM[Int](ex)
    successfulP.trySuccess(i)
    val p = successfulP.followedByWith(t => f)
    if (i > 1) nestedFollowedByWithsStm(p, i - 1, ex) else p
  }
}