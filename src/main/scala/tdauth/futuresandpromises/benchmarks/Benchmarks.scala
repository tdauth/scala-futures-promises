package tdauth.futuresandpromises.benchmarks

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Success

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.FP
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.cas.PrimCAS
import tdauth.futuresandpromises.mvar.PrimMVar
import tdauth.futuresandpromises.stm.PrimSTM

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
    var notFull = true
    while (notFull) {
      lock.lock()
      try {
        notFull = counter < max
        if (notFull) condition.await
      } finally {
        lock.unlock();
      }
    }
  }
}

/**
 * Compares the performance of our FP implementation to the performance of Scala FP and Twitter Util.
 */
object Benchmarks extends App {
  val CORES = 1

  val PERF1_HIGH_CONTENTION_N = 3
  val PERF1_HIGH_CONTENTION_M = 3
  val PERF1_HIGH_CONTENTION_K = 3
  val PERF1_LOW_CONTENTION_N = 3
  val PERF1_LOW_CONTENTION_M = 3
  val PERF1_LOW_CONTENTION_K = 3
  val PERF2_N = 3
  val PERF3_N = 3

  // Scala FP
  perf1ScalaFP(PERF1_HIGH_CONTENTION_N, PERF1_HIGH_CONTENTION_M, PERF1_HIGH_CONTENTION_K, CORES)
  perf1ScalaFP(PERF1_LOW_CONTENTION_N, PERF1_LOW_CONTENTION_M, PERF1_LOW_CONTENTION_K, CORES)
  perf2ScalaFP(PERF2_N, CORES)
  perf3ScalaFP(PERF3_N, CORES)

  // Prim CAS
  perf1Prim(PERF1_HIGH_CONTENTION_N, PERF1_HIGH_CONTENTION_M, PERF1_HIGH_CONTENTION_K, CORES, ex => new PrimCAS(ex))
  perf1Prim(PERF1_LOW_CONTENTION_N, PERF1_LOW_CONTENTION_M, PERF1_LOW_CONTENTION_K, CORES, ex => new PrimCAS(ex))
  perf2Prim(PERF2_N, CORES, ex => new PrimCAS(ex))
  perf3Prim(PERF3_N, CORES, ex => new PrimCAS(ex))

  // Prim MVar
  perf1Prim(PERF1_HIGH_CONTENTION_N, PERF1_HIGH_CONTENTION_M, PERF1_HIGH_CONTENTION_K, CORES, ex => new PrimMVar(ex))
  perf1Prim(PERF1_LOW_CONTENTION_N, PERF1_LOW_CONTENTION_M, PERF1_LOW_CONTENTION_K, CORES, ex => new PrimMVar(ex))
  perf2Prim(PERF2_N, CORES, ex => new PrimMVar(ex))
  perf3Prim(PERF3_N, CORES, ex => new PrimMVar(ex))

  // Prim STM
  perf1Prim(PERF1_HIGH_CONTENTION_N, PERF1_HIGH_CONTENTION_M, PERF1_HIGH_CONTENTION_K, CORES, ex => new PrimSTM(ex))
  perf1Prim(PERF1_LOW_CONTENTION_N, PERF1_LOW_CONTENTION_M, PERF1_LOW_CONTENTION_K, CORES, ex => new PrimSTM(ex))
  perf2Prim(PERF2_N, CORES, ex => new PrimSTM(ex))
  perf3Prim(PERF3_N, CORES, ex => new PrimSTM(ex))

  def getScalaFPExecutor(n: Int): Tuple2[ExecutorService, ExecutionContext] = {
    val executionService = Executors.newFixedThreadPool(n)
    (executionService, ExecutionContext.fromExecutorService(executionService))
  }

  def getPrimExecutor(n: Int): Executor = {
    val executionService = Executors.newFixedThreadPool(n)
    new JavaExecutor(executionService)
  }

  /*
    -- For each promise pi taken from [p1,...,pn]
--     m times onComplete pi incCount
--     k times tryComplete pi v
--     get pi
-- wait for counter to reach n*m (all onCompletes are processed)
*/
  def perf1ScalaFP(n: Int, m: Int, k: Int, cores: Int) {
    val counter = new Synchronizer(n * m)
    val ex = getScalaFPExecutor(cores)
    val executionService = ex._1
    val executionContext = ex._2

    val promises = (0 until n).map(_ => scala.concurrent.Promise.apply[Int])

    promises.foreach(p => {
      0 until m foreach (_ => {
        executionService.submit(new Runnable {
          override def run(): Unit = p.future.onComplete(t => counter.increment)(executionContext)
        })
      })
      0 until k foreach (_ => {
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

  def perf2ScalaFP(n: Int, cores: Int) {
    val ex = getScalaFPExecutor(cores)
    val executionService = ex._1
    val executionContext = ex._2

    val promises = (0 until n).map(_ => scala.concurrent.Promise.apply[Int])

    def registerOnComplete(p1: scala.concurrent.Promise[Int], p2: scala.concurrent.Promise[Int], rest: Seq[scala.concurrent.Promise[Int]]) {
      p1.future.onComplete(t => {
        if (p2 != null) {
          p2.trySuccess(1)

          val head = if (!rest.isEmpty) rest.head else null
          val tail = if (!rest.isEmpty) rest.tail else null
          registerOnComplete(p2, head, tail)
        }
      })(executionContext)
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).trySuccess(1)
    Await.result(promises.last.future, Duration.Inf)
    executionService.shutdownNow
  }

  def perf3ScalaFP(n: Int, cores: Int) {
    val counter = new Synchronizer(n)
    val ex = getScalaFPExecutor(cores)
    val executionService = ex._1
    val executionContext = ex._2

    val promises = (0 until n).map(_ => scala.concurrent.Promise.apply[Int])

    def registerOnComplete(p1: scala.concurrent.Promise[Int], p2: scala.concurrent.Promise[Int], rest: Seq[scala.concurrent.Promise[Int]]) {
      p1.future.onComplete(t => {
        if (p2 != null) {
          p2.trySuccess(1)
        }
        counter.increment
      })(executionContext)

      if (p2 != null) {
        val head = if (!rest.isEmpty) rest.head else null
        val tail = if (!rest.isEmpty) rest.tail else null
        registerOnComplete(p2, head, tail)
      }
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).trySuccess(1)
    Await.result(promises.last.future, Duration.Inf)
    executionService.shutdownNow
  }

  def perf1Prim(n: Int, m: Int, k: Int, cores: Int, f: (Executor) => FP[Int]) {
    val counter = new Synchronizer(n * m)
    val ex = getPrimExecutor(cores)

    val promises = (0 until n).map(_ => f.apply(ex))

    promises.foreach(p => {
      0 until m foreach (_ => ex.submit(() => p.onComplete(t => counter.increment)))
      0 until k foreach (_ => ex.submit(() => p.trySuccess(1)))
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
  def perf2Prim(n: Int, cores: Int, f: (Executor) => FP[Int]) {
    val ex = getPrimExecutor(cores)
    val promises = (0 until n).map(_ => f.apply(ex))

    def registerOnComplete(p1: FP[Int], p2: FP[Int], rest: Seq[FP[Int]]) {
      p1.onComplete(t => {
        if (p2 != null) {
          p2.trySuccess(1)
          val head = if (!rest.isEmpty) rest.head else null
          val tail = if (!rest.isEmpty) rest.tail else null
          registerOnComplete(p2, head, tail)
        }
      })
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).trySuccess(1)
    promises.last.get
    ex.shutdown
  }

  /*
   -- chain of oncomplete and tries
-- no excessive nesting
-- do onComplete p1 (tryComplete p2)
--    onComplete p2 (tryComplete p3)
--    ...
---   tryComplete p1
   */
  def perf3Prim(n: Int, cores: Int, f: (Executor) => FP[Int]) {
    val ex = getPrimExecutor(cores)
    val promises = (0 until n).map(_ => f.apply(ex))

    def registerOnComplete(p1: FP[Int], p2: FP[Int], rest: Seq[FP[Int]]) {
      p1.onComplete(t => {
        if (p2 != null) {
          p2.trySuccess(1)
        }
      })

      if (p2 != null) {
        val head = if (!rest.isEmpty) rest.head else null
        val tail = if (!rest.isEmpty) rest.tail else null
        registerOnComplete(p2, head, tail)
      }
    }

    registerOnComplete(promises(0), promises(1), promises.drop(2))

    promises(0).trySuccess(1)
    promises.last.get
    ex.shutdown
  }
}