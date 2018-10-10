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

/**
 * Compares the performance of our FP implementation to the performance of Scala FP and Twitter Util.
 */
object FPPerformanceTest extends Bench.OfflineReport {
  val NUMBER_OF_NEW_PS: Gen[Int] = Gen.range("size")(1000, 10000, 1000)
  val NUMBER_OF_TRY_COMPLETE: Gen[Int] = Gen.range("size")(1000, 10000, 1000)
  val NUMBER_OF_ON_COMPLETES: Gen[Int] = Gen.range("size")(10, 100, 10)
  val NUMBER_OF_FOLLOWED_BYS: Gen[Int] = Gen.range("size")(100, 1000, 100)

  // TODO #29 Do we even need executors and if so should we scale the number of threads?
  val executionService = Executors.newSingleThreadExecutor()
  val primExecutor = new JavaExecutor(executionService)
  val executionContext = ExecutionContext.fromExecutorService(executionService)
  // TODO #29 Executor for Twitter?
  // See https://groups.google.com/forum/#!topic/finaglers/ovDL2UFKoDw
  val twitterExecutor = com.twitter.util.FuturePool.apply(executionService)

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
            promises.foreach(p => p.updateIfEmpty(Return(10)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = com.twitter.util.Promise.apply[Int]
            registerOnCompletesTwitter(p, r)
            p.updateIfEmpty(Return(10))
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
            p.updateIfEmpty(Return(10))
            com.twitter.util.Await.ready(p)
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
            promises.foreach(p => p.tryComplete(Success(10)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = scala.concurrent.Promise.apply[Int]
            val f = p.future
            registerOnCompletesScalaFP(f, r)
            p.trySuccess(10)
            Await.ready(f, Duration.Inf)
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            val p = scala.concurrent.Promise.apply[Int]
            val f = p.future
            val nested = nestedMap(f, r)
            p.trySuccess(10)
            Await.ready(f, Duration.Inf)
          }
      }
    }
  }

  performance of "PrimCAS" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield new PrimCAS[Int](primExecutor)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimCAS[Int](primExecutor)
            promises.foreach(p => p.tryComplete(new Try[Int](10)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = new PrimCAS[Int](primExecutor)
            registerOnCompletes(p, r)
            p.trySuccess(10)
            p.get
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            val p = new PrimCAS[Int](primExecutor)
            val nested = nestedFollowedBy(p, r)
            p.trySuccess(10)
            nested.get
          }
      }
    }
  }

  performance of "PrimMVar" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield new PrimMVar[Int](primExecutor)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimMVar[Int](primExecutor)
            promises.foreach(p => p.tryComplete(new Try[Int](10)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = new PrimMVar[Int](primExecutor)
            registerOnCompletes(p, r)
            p.trySuccess(10)
            p.get
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            val p = new PrimMVar[Int](primExecutor)
            val nested = nestedFollowedBy(p, r)
            p.trySuccess(10)
            nested.get
          }
      }
    }
  }

  performance of "PrimSTM" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield new PrimSTM[Int](primExecutor)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimSTM[Int](primExecutor)
            promises.foreach(p => p.tryComplete(new Try[Int](10)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = new PrimSTM[Int](primExecutor)
            registerOnCompletes(p, r)
            p.trySuccess(10)
            p.get
          }
      }
    }

    measure method "followedBy" in {
      using(NUMBER_OF_FOLLOWED_BYS) in {
        r =>
          {
            val p = new PrimSTM[Int](primExecutor)
            val nested = nestedFollowedBy(p, r)
            p.trySuccess(10)
            nested.get
          }
      }
    }
  }

  // TODO #29 Use twitterExecutor
  @tailrec def registerOnCompletesTwitter(f: com.twitter.util.Future[Int], i: Int) {
    f.respond(t => t.get())
    if (i > 0) registerOnCompletesTwitter(f, i - 1)
  }

  // TODO #29 Use twitterExecutor
  @tailrec def nestedFollowedByTwitter(f: com.twitter.util.Future[Int], i: Int): com.twitter.util.Future[Int] = {
    val p = f.map(t => t + 1)
    if (i > 0) nestedFollowedByTwitter(p, i - 1) else p
  }

  @tailrec def registerOnCompletesScalaFP(f: scala.concurrent.Future[Int], i: Int) {
    f.onComplete(t => t.get)(executionContext)
    if (i > 0) registerOnCompletesScalaFP(f, i - 1)
  }

  @tailrec def nestedMap(f: scala.concurrent.Future[Int], i: Int): scala.concurrent.Future[Int] = {
    val p = f.map(t => t + 1)(executionContext)
    if (i > 0) nestedMap(p, i - 1) else p
  }

  @tailrec def registerOnCompletes(f: FP[Int], i: Int) {
    f.onComplete(t => t.get())
    if (i > 0) registerOnCompletes(f, i - 1)
  }

  @tailrec def nestedFollowedBy(f: FP[Int], i: Int): FP[Int] = {
    val p = f.followedBy(t => t + 1)
    if (i > 0) nestedFollowedBy(p, i - 1) else p
  }
}