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
    do {
      lock.lock()
      try {
        notFull = counter < max
        if (notFull) condition.await
      } finally {
        lock.unlock();
      }
    } while (notFull)
  }
}

/**
 * Compares the performance of our FP implementation to the performance of Scala FP and Twitter Util.
 */
object Benchmarks extends App {
  val ITERATIONS = 10
  val CORES = Vector(1, 2, 4, 8)

  // test 1
  val TEST_1_N = 10000
  val TEST_1_M = 100
  val TEST_1_K = 200
  // test 2
  val TEST_2_N = 100000
  val TEST_2_M = 20
  val TEST_2_K = 2
  // test 3
  val TEST_3_N = 2000000
  // test 4
  val TEST_4_N = 2000000

  runAllTests

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    printf("Elapsed time: " + (t1 - t0) + "ns")
    result
  }

  def execTest(t: () => Unit): Double = {
    System.gc
    val start = System.nanoTime()
    t()
    val fin = System.nanoTime()
    val result = (fin - start)
    val seconds = result.toDouble / 1000000000.0
    printf("Time: %.2fs\n", seconds)
    seconds
  }

  def runTest(n: Int, t: () => Unit) {
    val rs = for (i <- (1 to n)) yield execTest(t)
    val xs = rs.sorted
    val low = xs.head
    val high = xs.last
    val m = xs.length.toDouble
    val av = xs.sum / m
    printf("low: %.2fs high: %.2fs avrg: %.2fs\n", low, high, av)
  }

  def runAll(n: Int, t0: () => Unit, t1: () => Unit, t2: () => Unit, t3: () => Unit, t4: () => Unit): Unit = {
    println("Twitter Util")
    runTest(n, t0)
    println("Scala FP")
    runTest(n, t1)
    println("Prim CAS")
    runTest(n, t2)
    println("Prim MVar")
    runTest(n, t3)
    println("Prim STM")
    runTest(n, t4)
  }

  def test1(cores: Int) {
    val n = TEST_1_N
    val m = TEST_1_M
    val k = TEST_1_K
    runAll(
      ITERATIONS,
      () => perf1TwitterUtil(n, m, k, cores),
      () => perf1ScalaFP(n, m, k, cores),
      () => perf1Prim(n, m, k, cores, ex => new PrimCAS(ex)),
      () => perf1Prim(n, m, k, cores, ex => new PrimMVar(ex)),
      () => perf1Prim(n, m, k, cores, ex => new PrimSTM(ex)))
  }

  def test2(cores: Int) {
    val n = TEST_2_N
    val m = TEST_2_M
    val k = TEST_2_K
    runAll(
      ITERATIONS,
      () => perf1TwitterUtil(n, m, k, cores),
      () => perf1ScalaFP(n, m, k, cores),
      () => perf1Prim(n, m, k, cores, ex => new PrimCAS(ex)),
      () => perf1Prim(n, m, k, cores, ex => new PrimMVar(ex)),
      () => perf1Prim(n, m, k, cores, ex => new PrimSTM(ex)))
  }

  def test3(cores: Int) {
    val n = TEST_3_N
    runAll(
      ITERATIONS,
      () => perf2TwitterUtil(n, cores),
      () => perf2ScalaFP(n, cores),
      () => perf2Prim(n, cores, ex => new PrimCAS(ex)),
      () => perf2Prim(n, cores, ex => new PrimMVar(ex)),
      () => perf2Prim(n, cores, ex => new PrimSTM(ex)))
  }

  def test4(cores: Int) {
    val n = TEST_4_N
    runAll(
      ITERATIONS,
      () => perf3TwitterUtil(n, cores),
      () => perf3ScalaFP(n, cores),
      () => perf3Prim(n, cores, ex => new PrimCAS(ex)),
      () => perf3Prim(n, cores, ex => new PrimMVar(ex)),
      () => perf3Prim(n, cores, ex => new PrimSTM(ex)))
  }

  def runTestForCores(name: String, t: (Int) => Unit) {
    val nameSeparator = "=" * 40
    println(nameSeparator)
    println(name)
    println(nameSeparator)
    val coresSeparator = "-" * 40

    CORES.foreach(c => {
      println(coresSeparator)
      println("Cores: " + c)
      println(coresSeparator)
      t(c)
    })
  }

  def runTest1 = runTestForCores("Test 1", test1)
  def runTest2 = runTestForCores("Test 2", test2)
  def runTest3 = runTestForCores("Test 3", test3)
  def runTest4 = runTestForCores("Test 4", test4)

  def runAllTests {
    runTest1
    runTest2
    runTest3
    runTest4
  }

  def getTwitterUtilExecutor(n: Int) = com.twitter.util.FuturePool(Executors.newFixedThreadPool(n))

  def getScalaFPExecutor(n: Int): Tuple2[ExecutorService, ExecutionContext] = {
    val executionService = Executors.newFixedThreadPool(n)
    (executionService, ExecutionContext.fromExecutorService(executionService))
  }

  def getPrimExecutor(n: Int): Executor = {
    val executionService = Executors.newFixedThreadPool(n)
    new JavaExecutor(executionService)
  }

  def perf1TwitterUtil(n: Int, m: Int, k: Int, cores: Int) {
    val counter = new Synchronizer(n * m)
    val ex = getTwitterUtilExecutor(cores)

    val promises = (1 to n).map(_ => com.twitter.util.Promise[Int])

    promises.foreach(p => {
      1 to m foreach (_ => {
        ex.executor.submit(new Runnable {
          override def run(): Unit = p.respond(t => counter.increment) // TODO How to pass ex, so the callback is submitted to the executor?
        })
      })
      1 to k foreach (_ => {
        ex.executor.submit(new Runnable {
          override def run(): Unit = p.updateIfEmpty(com.twitter.util.Return(1))
        })
      })
    })

    // get ps
    promises.foreach(p => com.twitter.util.Await.result(p)) // TODO Try it without get

    // wait for counter to reach n*m
    counter.await
    ex.executor.shutdownNow
  }

  def perf2TwitterUtil(n: Int, cores: Int) {
    val ex = getTwitterUtilExecutor(cores)

    val promises = (1 to n).map(_ => com.twitter.util.Promise[Int])

    def registerOnComplete(rest: Seq[com.twitter.util.Promise[Int]]) {
      val p1 = if (rest.size > 0) rest(0) else null
      val p2 = if (rest.size > 1) rest(1) else null
      if (p1 ne null) {
        p1.respond(t => {
          if (p2 ne null) {
            p2.setValue(1)
            registerOnComplete(rest.tail)
          }
        }) // TODO How to pass ex, so the callback is submitted to the executor?
      }
    }

    registerOnComplete(promises)

    promises(0).setValue(1)
    com.twitter.util.Await.result(promises.last)
    ex.executor.shutdownNow
  }

  def perf3TwitterUtil(n: Int, cores: Int) {
    val ex = getTwitterUtilExecutor(cores)

    val promises = (1 to n).map(_ => com.twitter.util.Promise[Int])

    def registerOnComplete(rest: Seq[com.twitter.util.Promise[Int]]) {
      val p1 = if (rest.size > 0) rest(0) else null
      val p2 = if (rest.size > 1) rest(1) else null
      if (p1 ne null) {
        p1.respond(t => {
          if (p2 ne null) {
            p2.setValue(1)
          }
        }) // TODO How to pass ex, so the callback is submitted to the executor?

        if (p2 ne null) {
          registerOnComplete(rest.tail)
        }
      }
    }

    registerOnComplete(promises)

    promises(0).setValue(1)
    com.twitter.util.Await.result(promises.last)
    ex.executor.shutdownNow
  }

  def perf1ScalaFP(n: Int, m: Int, k: Int, cores: Int) {
    val counter = new Synchronizer(n * m)
    val ex = getScalaFPExecutor(cores)
    val executionService = ex._1
    val executionContext = ex._2

    val promises = (1 to n).map(_ => scala.concurrent.Promise[Int])

    promises.foreach(p => {
      1 to m foreach (_ => {
        executionService.submit(new Runnable {
          override def run(): Unit = p.future.onComplete(t => counter.increment)(executionContext)
        })
      })
      1 to k foreach (_ => {
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

    val promises = (1 to n).map(_ => scala.concurrent.Promise[Int])

    def registerOnComplete(rest: Seq[scala.concurrent.Promise[Int]]) {
      val p1 = if (rest.size > 0) rest(0) else null
      val p2 = if (rest.size > 1) rest(1) else null
      if (p1 ne null) {
        p1.future.onComplete(t => {
          if (p2 ne null) {
            p2.trySuccess(1)
            registerOnComplete(rest.tail)
          }
        })(executionContext)
      }
    }

    registerOnComplete(promises)

    promises(0).trySuccess(1)
    Await.result(promises.last.future, Duration.Inf)
    executionService.shutdownNow
  }

  def perf3ScalaFP(n: Int, cores: Int) {
    val ex = getScalaFPExecutor(cores)
    val executionService = ex._1
    val executionContext = ex._2

    val promises = (1 to n).map(_ => scala.concurrent.Promise[Int])

    def registerOnComplete(rest: Seq[scala.concurrent.Promise[Int]]) {
      val p1 = if (rest.size > 0) rest(0) else null
      val p2 = if (rest.size > 1) rest(1) else null
      if (p1 ne null) {
        p1.future.onComplete(t => {
          if (p2 ne null) {
            p2.trySuccess(1)
          }
        })(executionContext)

        if (p2 ne null) {
          registerOnComplete(rest.tail)
        }
      }
    }

    registerOnComplete(promises)

    promises(0).trySuccess(1)
    Await.result(promises.last.future, Duration.Inf)
    executionService.shutdownNow
  }

  def perf1Prim(n: Int, m: Int, k: Int, cores: Int, f: (Executor) => FP[Int]) {
    val counter = new Synchronizer(n * m)
    val ex = getPrimExecutor(cores)

    val promises = (1 to n).map(_ => f(ex))

    promises.foreach(p => {
      1 to m foreach (_ => ex.submit(() => p.onComplete(t => counter.increment)))
      1 to k foreach (_ => ex.submit(() => p.trySuccess(1)))
    })

    // get ps
    promises.foreach(p => p.get) // TODO Try it without get

    // wait for counter to reach n*m
    counter.await
    ex.shutdown
  }

  def perf2Prim(n: Int, cores: Int, f: (Executor) => FP[Int]) {
    val ex = getPrimExecutor(cores)
    val promises = (1 to n).map(_ => f(ex))

    def registerOnComplete(rest: Seq[FP[Int]]) {
      val p1 = if (rest.size > 0) rest(0) else null
      val p2 = if (rest.size > 1) rest(1) else null
      if (p1 ne null) {
        p1.onComplete(t => {
          if (p2 ne null) {
            p2.trySuccess(1)
            registerOnComplete(rest.tail)
          }
        })
      }
    }

    registerOnComplete(promises)

    promises(0).trySuccess(1)
    promises.last.get
    ex.shutdown
  }

  def perf3Prim(n: Int, cores: Int, f: (Executor) => FP[Int]) {
    val ex = getPrimExecutor(cores)
    val promises = (1 to n).map(_ => f(ex))

    def registerOnComplete(rest: Seq[FP[Int]]) {
      val p1 = if (rest.size > 0) rest(0) else null
      val p2 = if (rest.size > 1) rest(1) else null
      if (p1 ne null) {
        p1.onComplete(t => {
          if (p2 ne null) {
            p2.trySuccess(1)
          }
        })

        if (p2 ne null) {
          registerOnComplete(rest.tail)
        }
      }
    }

    registerOnComplete(promises)

    promises(0).trySuccess(1)
    promises.last.get
    ex.shutdown
  }
}