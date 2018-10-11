package tdauth.futuresandpromises

import java.util.concurrent.Executors

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Success

import org.scalameter.api.Bench
import org.scalameter.api.Gen
import org.scalameter.picklers.Implicits

import com.twitter.util.Return

import tdauth.futuresandpromises.cas.PrimCAS
import tdauth.futuresandpromises.mvar.PrimMVar
import tdauth.futuresandpromises.stm.PrimSTM
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.ReentrantLock
import org.scalameter.picklers.Pickler

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
 */
object FPPerformanceTest extends Bench.OfflineReport {
  val axisCores = Gen.range("cores")(_, _, _)
  val CORES_RANGE = axisCores(1, 8, 1)

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
    measure method "perf1HighContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1TwitterUtil(10000, 100, 200, cores)
      }
    }

    measure method "perf1LowContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1TwitterUtil(100000, 20, 2, cores)
      }
    }

    measure method "perf2" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf2TwitterUtil(2000000, cores)
      }
    }

    measure method "perf3" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf3TwitterUtil(2000000, cores)
      }
    }
  }

  performance of "Scala FP" in {
    measure method "perf1HighContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1ScalaFP(10000, 100, 200, cores)
      }
    }

    measure method "perf1LowContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1ScalaFP(100000, 20, 2, cores)
      }
    }

    measure method "perf2" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf2ScalaFP(2000000, cores)
      }
    }

    measure method "perf3" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf3ScalaFP(2000000, cores)
      }
    }
  }

  performance of "PrimCAS" in {
    measure method "perf1HighContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1Prim(10000, 100, 200, cores, ex => new PrimCAS(ex))
      }
    }

    measure method "perf1LowContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1Prim(100000, 20, 2, cores, ex => new PrimCAS(ex))
      }
    }

    measure method "perf2" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf2Prim(2000000, cores, ex => new PrimCAS(ex))
      }
    }

    measure method "perf3" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf3Prim(2000000, cores, ex => new PrimCAS(ex))
      }
    }
  }

  performance of "PrimMVar" in {
    measure method "perf1HighContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1Prim(10000, 100, 200, cores, ex => new PrimMVar(ex))
      }
    }

    measure method "perf1LowContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1Prim(100000, 20, 2, cores, ex => new PrimMVar(ex))
      }
    }

    measure method "perf2" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf2Prim(2000000, cores, ex => new PrimMVar(ex))
      }
    }

    measure method "perf3" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf3Prim(2000000, cores, ex => new PrimMVar(ex))
      }
    }
  }

  performance of "PrimSTM" in {
    measure method "perf1HighContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1Prim(10000, 100, 200, cores, ex => new PrimSTM(ex))
      }
    }

    measure method "perf1LowContention" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf1Prim(100000, 20, 2, cores, ex => new PrimSTM(ex))
      }
    }

    measure method "perf2" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf2Prim(2000000, cores, ex => new PrimSTM(ex))
      }
    }

    measure method "perf3" in {
      using(CORES_RANGE) in {
        cores => sulzmannPerf3Prim(2000000, cores, ex => new PrimSTM(ex))
      }
    }
  }

  /*
    -- For each promise pi taken from [p1,...,pn]
--     m times onComplete pi incCount
--     k times tryComplete pi v
--     get pi
-- wait for counter to reach n*m (all onCompletes are processed)
*/
  def sulzmannPerf1TwitterUtil(n: Int, m: Int, k: Int, cores: Int) {
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

  def sulzmannPerf2TwitterUtil(n: Int, cores: Int) {
    // TODO Use Twitter executor

    val promises = for (i <- 0 to n) yield com.twitter.util.Promise.apply[Int]

    def registerOnComplete(p1: com.twitter.util.Promise[Int], p2: com.twitter.util.Promise[Int], rest: Seq[com.twitter.util.Promise[Int]]) {
      p1.respond(t => {
        if (p2 != null) {
          p2.updateIfEmpty(Return(1))
          registerOnComplete(p2, rest.head, rest.tail)
        }
      })
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).updateIfEmpty(Return(1))
    com.twitter.util.Await.result(promises.last)
  }

  def sulzmannPerf3TwitterUtil(n: Int, cores: Int) {
    // TODO Use Twitter Util executor

    val promises = for (i <- 0 to n) yield com.twitter.util.Promise.apply[Int]

    def registerOnComplete(p1: com.twitter.util.Promise[Int], p2: com.twitter.util.Promise[Int], rest: Seq[com.twitter.util.Promise[Int]]) {
      p1.respond(t => {
        if (p2 != null) {
          p2.updateIfEmpty(Return(1))
        }
      })

      if (p2 != null) registerOnComplete(p2, rest.head, rest.tail)
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).updateIfEmpty(Return(1))
    com.twitter.util.Await.result(promises.last)
  }

  def sulzmannPerf1ScalaFP(n: Int, m: Int, k: Int, cores: Int) {
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

  def sulzmannPerf2ScalaFP(n: Int, cores: Int) {
    val ex = getScalaFPExecutor(cores)
    val executionService = ex._1
    val executionContext = ex._2

    val promises = for (i <- 0 to n) yield scala.concurrent.Promise.apply[Int]

    def registerOnComplete(p1: scala.concurrent.Promise[Int], p2: scala.concurrent.Promise[Int], rest: Seq[scala.concurrent.Promise[Int]]) {
      p1.future.onComplete(t => {
        if (p2 != null) {
          p2.trySuccess(1)
          registerOnComplete(p2, rest.head, rest.tail)
        }
      })(executionContext)
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).trySuccess(1)
    Await.result(promises.last.future, Duration.Inf)
    executionService.shutdownNow
  }

  def sulzmannPerf3ScalaFP(n: Int, cores: Int) {
    val ex = getScalaFPExecutor(cores)
    val executionService = ex._1
    val executionContext = ex._2

    val promises = for (i <- 0 to n) yield scala.concurrent.Promise.apply[Int]

    def registerOnComplete(p1: scala.concurrent.Promise[Int], p2: scala.concurrent.Promise[Int], rest: Seq[scala.concurrent.Promise[Int]]) {
      p1.future.onComplete(t => {
        if (p2 != null) {
          p2.trySuccess(1)
        }
      })(executionContext)

      if (p2 != null) registerOnComplete(p2, rest.head, rest.tail)
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).trySuccess(1)
    Await.result(promises.last.future, Duration.Inf)
    executionService.shutdownNow
  }

  def sulzmannPerf1Prim(n: Int, m: Int, k: Int, cores: Int, f: (Executor) => FP[Int]) {
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

  /*
   -- chain of (nested) oncomplete and tries
--  onComplete p1 (do tryComplete p2
--                    onComplete p2 (do tryComplete p3
--                                      ...            ))
--  tryComplete p1
   */
  def sulzmannPerf2Prim(n: Int, cores: Int, f: (Executor) => FP[Int]) {
    val ex = getPrimExecutor(cores)

    val promises = for (i <- 0 to n) yield f.apply(ex)

    def registerOnComplete(p1: FP[Int], p2: FP[Int], rest: Seq[FP[Int]]) {
      p1.onComplete(t => {
        if (p2 != null) {
          p2.trySuccess(1)
          registerOnComplete(p2, rest.head, rest.tail)
        }
      })
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).trySuccess(1)
    promises.last.get
  }

  /*
   -- chain of oncomplete and tries
-- no excessive nesting
-- do onComplete p1 (tryComplete p2)
--    onComplete p2 (tryComplete p3)
--    ...
---   tryComplete p1
   */
  def sulzmannPerf3Prim(n: Int, cores: Int, f: (Executor) => FP[Int]) {
    val ex = getPrimExecutor(cores)

    val promises = for (i <- 0 to n) yield f.apply(ex)

    def registerOnComplete(p1: FP[Int], p2: FP[Int], rest: Seq[FP[Int]]) {
      p1.onComplete(t => {
        if (p2 != null) {
          p2.trySuccess(1)
        }
      })

      if (p2 != null) registerOnComplete(p2, rest.head, rest.tail)
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).trySuccess(1)
    promises.last.get
  }
}