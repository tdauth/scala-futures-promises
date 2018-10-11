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
import java.util.concurrent.locks.ReentrantLock

/**
 * Waits until the counter has reached max.
 */
class Synchronizer(max: Int) {
  var lock = new ReentrantLock()
  var condition = lock.newCondition()
  var counter = 0

  def increment {
    lock.lock()
    try {
      counter = counter + 1
      condition.signal
    } finally {
      lock.unlock();
    }
  }

  def await {
    lock.lock()
    try {
      if (counter < max) condition.await
    } finally {
      lock.unlock();
    }
  }
}

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
  val NUMBER_OF_ON_COMPLETES = Gen.range("size")(100, 1000, 100)
  val NUMBER_OF_FOLLOWED_BYS = Gen.range("size")(100, 1000, 100)
  val NUMBER_OF_FOLLOWED_BY_WITHS = Gen.range("size")(1000, 10000, 1000)

  /*
   n number of promises
   m * n number of oncomplete runs
   k * n number of try completes
  */

  /*
   high contention case
   n= 10000
   m= 100
   k= 200
   */
  val SULZMANN_HIGH_CONTENTION_CASE = for {
    n <- Gen.range("n")(10000, 10000, 100)
    m <- Gen.range("m")(100, 100, 100)
    k <- Gen.range("k")(200, 200, 100)
    cores <- Gen.range("cores")(1, 8, 1)
  } yield (n, m, k, cores)

  /*
   n= 100000
   m= 20
   k= 2
   */
  val SULZMANN_LOWER_CONTENTION_CASE = for {
    n <- Gen.range("n")(100000, 100000, 100)
    m <- Gen.range("m")(20, 20, 100)
    k <- Gen.range("k")(2, 2, 100)
    cores <- Gen.range("cores")(1, 8, 1)
  } yield (n, m, k, cores)

  // TODO #29 Do we even need executors and if so should we scale the number of threads?
  val executionService = Executors.newSingleThreadExecutor()
  // TODO #29 Executor for Twitter?
  // See https://groups.google.com/forum/#!topic/finaglers/ovDL2UFKoDw
  val twitterExecutor = com.twitter.util.FuturePool.apply(executionService)

  def getTwitterUtilExecutor(n: Int): ExecutorService = Executors.newFixedThreadPool(n)

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

    measure method "sulzmann1HighContentionCase" in {
      using(SULZMANN_HIGH_CONTENTION_CASE) in {
        r => sulzmannPerf1TwitterUtil(r)
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

    measure method "sulzmann1HighContentionCase" in {
      using(SULZMANN_HIGH_CONTENTION_CASE) in {
        r => sulzmannPerf1ScalaFP(r)
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

    measure method "sulzmann1HighContentionCase" in {
      using(SULZMANN_HIGH_CONTENTION_CASE) in {
        r => sulzmannPerf1Prim(r, ex => new PrimCAS(ex))
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

    measure method "sulzmann1HighContentionCase" in {
      using(SULZMANN_HIGH_CONTENTION_CASE) in {
        r => sulzmannPerf1Prim(r, ex => new PrimMVar(ex))
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

    measure method "sulzmann1HighContentionCase" in {
      using(SULZMANN_HIGH_CONTENTION_CASE) in {
        r => sulzmannPerf1Prim(r, ex => new PrimSTM(ex))
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

  /*
    -- For each promise pi taken from [p1,...,pn]
--     m times onComplete pi incCount
--     k times tryComplete pi v
--     get pi
-- wait for counter to reach n*m (all onCompletes are processed)
*/
  def sulzmannPerf1TwitterUtil(r: Tuple4[Int, Int, Int, Int]) {
    val n = r._1
    val m = r._2
    val k = r._3
    val cores = r._4
    val counter = new Synchronizer(n * m)
    val ex = getTwitterUtilExecutor(cores)

    val promises = for (i <- 0 to n) yield com.twitter.util.Promise.apply[Int]

    promises.foreach(p => {
      0 to m foreach (_ => {
        ex.submit(new Runnable {
          // TODO submit the callback to the executor WHEN IT IS CALLED, in Haskell we use forkIO
          override def run = p.respond(t => counter.increment)
        })
      })
      0 to k foreach (_ => {
        ex.submit(new Runnable {
          override def run = p.updateIfEmpty(Return(1))
        })
      })
    })

    // get ps
    promises.foreach(p => com.twitter.util.Await.result(p)) // TODO Try it without get

    // wait for counter to reach n*m
    counter.await
    ex.shutdownNow
  }

  def sulzmannPerf1ScalaFP(r: Tuple4[Int, Int, Int, Int]) {
    val n = r._1
    val m = r._2
    val k = r._3
    val cores = r._4
    val counter = new Synchronizer(n * m)
    val ex = getScalaFPExecutor(cores)
    val executionService = ex._1
    val executionContext = ex._2

    val promises = for (i <- 0 to n) yield scala.concurrent.Promise.apply[Int]

    promises.foreach(p => {
      0 to m foreach (_ => {
        executionService.submit(new Runnable {
          override def run(): Unit = p.future.onComplete(t => counter.increment)(executionContext)
        })
      })
      0 to k foreach (_ => {
        executionService.submit(new Runnable {
          override def run(): Unit = p.tryComplete(Success(1))
        })
      })
    })

    // get ps
    promises.foreach(p => Await.result(p.future, Duration.Inf)) // TODO Try it without get

    // wait for counter to reach n*m
    counter.await
    executionService.shutdownNow
  }

  def sulzmannPerf1Prim(r: Tuple4[Int, Int, Int, Int], f: (Executor) => FP[Int]) {
    val n = r._1
    val m = r._2
    val k = r._3
    val cores = r._4
    val counter = new Synchronizer(n * m)
    val ex = getPrimExecutor(cores)

    val promises = for (i <- 0 to n) yield f.apply(ex)

    promises.foreach(p => {
      0 to m foreach (_ => ex.submit(() => p.onComplete(t => counter.increment)))
      0 to k foreach (_ => ex.submit(() => p.trySuccess(1)))
    })

    // get ps
    promises.foreach(p => p.get) // TODO Try it without get

    // wait for counter to reach n*m
    counter.await
    ex.shutdown
  }
}