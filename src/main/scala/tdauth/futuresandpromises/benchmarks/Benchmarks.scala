package tdauth.futuresandpromises.benchmarks

import java.io.File
import java.io.FileWriter
import java.util.Locale
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
 *
 * TODO What about:
 * - https://github.com/scalaz/scalaz/blob/series/7.3.x/concurrent/src/main/scala/scalaz/concurrent/MVar.scala
 * - https://github.com/scalaz/scalaz/blob/series/7.3.x/concurrent/src/main/scala/scalaz/concurrent/Future.scala
 * - https://github.com/scalaz/scalaz/blob/series/7.3.x/concurrent/src/main/scala/scalaz/concurrent/Chan.scala
 */
object Benchmarks extends App {
  val ITERATIONS = 10
  val CORES = Vector(1, 2, 4, 8) // +1 for the main thread!

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

  deletePlotFiles

  runAllTests

  def getPlotFileName(testNumber: Int, plotFileSuffix: String) = "test" + testNumber + "_scala_" + plotFileSuffix + ".dat"

  def deletePlotFiles() {
    val files = for {
      testNumber <- Vector(1, 2, 3, 4)
      plotFileSuffix <- Vector("twitterutil", "scalafp", "cas", "mvar", "stm")

    } yield new File(getPlotFileName(testNumber, plotFileSuffix))
    files.filter(_ exists).foreach(_ delete)
  }

  def writeEntryIntoPlotFile(plotFilePath: String, cores: Int, time: Double) {
    val fileWriter = new FileWriter(plotFilePath, true)
    try {
      fileWriter.append("%d  %.2f\n".formatLocal(Locale.US, cores, time))
    } finally fileWriter.close()
  }

  /**
   * Measures the execution time of t and returns it in ns.
   * The returned value can be substracted from the total execution time to ignore it.
   */
  def benchmarkSuspend(t: => Unit): Long = {
    val start = System.nanoTime()
    t
    val fin = System.nanoTime()
    (fin - start)
  }

  /**
   * @param t The test function which returns time which has to be substracted from the exectuion time since it should not be measured.
   */
  def execTest(t: () => Long): Double = {
    System.gc
    val start = System.nanoTime()
    val difference = t()
    val fin = System.nanoTime()
    val result = (fin - start) - difference
    val seconds = result.toDouble / 1000000000.0
    printf("Time: %.2fs, Time in ns: %d, Excluded time in ns: %d\n", seconds, result, difference)
    seconds
  }

  def runTest(plotFileSuffix: String, testNumber: Int, cores: Int, t: () => Long) {
    val rs = for (i <- (1 to ITERATIONS)) yield execTest(t)
    val xs = rs.sorted
    val low = xs.head
    val high = xs.last
    val m = xs.length.toDouble
    val av = xs.sum / m
    printf("low: %.2fs high: %.2fs avrg: %.2fs\n", low, high, av)
    writeEntryIntoPlotFile(getPlotFileName(testNumber, plotFileSuffix), cores, av)
  }

  def runAll(testNumber: Int, cores: Int, t0: () => Long, t1: () => Long, t2: () => Long, t3: () => Long, t4: () => Long): Unit = {
    println("Twitter Util")
    runTest("twitterutil", testNumber, cores, t0)
    println("Scala FP")
    runTest("scalafp", testNumber, cores, t1)
    println("Prim CAS")
    runTest("cas", testNumber, cores, t2)
    println("Prim MVar")
    runTest("mvar", testNumber, cores, t3)
    println("Prim STM")
    runTest("stm", testNumber, cores, t4)
  }

  def test1(cores: Int) {
    val n = TEST_1_N
    val m = TEST_1_M
    val k = TEST_1_K
    runAll(
      1,
      cores,
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
      2,
      cores,
      () => perf1TwitterUtil(n, m, k, cores),
      () => perf1ScalaFP(n, m, k, cores),
      () => perf1Prim(n, m, k, cores, ex => new PrimCAS(ex)),
      () => perf1Prim(n, m, k, cores, ex => new PrimMVar(ex)),
      () => perf1Prim(n, m, k, cores, ex => new PrimSTM(ex)))
  }

  def test3(cores: Int) {
    val n = TEST_3_N
    runAll(
      3,
      cores,
      () => perf2TwitterUtil(n, cores),
      () => perf2ScalaFP(n, cores),
      () => perf2Prim(n, cores, ex => new PrimCAS(ex)),
      () => perf2Prim(n, cores, ex => new PrimMVar(ex)),
      () => perf2Prim(n, cores, ex => new PrimSTM(ex)))
  }

  def test4(cores: Int) {
    val n = TEST_4_N
    runAll(
      4,
      cores,
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

  def perf1TwitterUtil(n: Int, m: Int, k: Int, cores: Int): Long = {
    val counter = new Synchronizer(n * (m + k))
    var ex: com.twitter.util.ExecutorServiceFuturePool = null
    val difference = benchmarkSuspend { ex = getTwitterUtilExecutor(cores) }

    val promises = (1 to n).map(_ => com.twitter.util.Promise[Int])

    promises.foreach(p => {
      1 to m foreach (_ => {
        ex.executor.submit(new Runnable {
          /*
           * We cannot use respond for Twitter Util since there is no way of specifying the executor for the callback.
           * Without transform the benchmark performs much faster.
           */
          override def run(): Unit = p.transform(t => ex(counter.increment))
        })
      })
      1 to k foreach (_ => {
        ex.executor.submit(new Runnable {
          override def run(): Unit = {
            p.updateIfEmpty(com.twitter.util.Return(1))
            counter.increment
          }
        })
      })
    })

    // get ps
    promises.foreach(p => com.twitter.util.Await.result(p))

    counter.await

    difference + benchmarkSuspend { ex.executor.shutdownNow }
  }

  def perf2TwitterUtil(n: Int, cores: Int): Long = {
    var ex: com.twitter.util.ExecutorServiceFuturePool = null
    val difference = benchmarkSuspend { ex = getTwitterUtilExecutor(cores) }

    val promises = (1 to n).map(_ => com.twitter.util.Promise[Int])

    def registerOnComplete(rest: Seq[com.twitter.util.Promise[Int]]) {
      val p1 = if (rest.size > 0) rest(0) else null
      val p2 = if (rest.size > 1) rest(1) else null
      if (p1 ne null) {
        /*
         * We cannot use respond for Twitter Util since there is no way of specifying the executor for the callback.
         */
        p1.transform(t =>
          ex({
            if (p2 ne null) {
              p2.setValue(1)
              registerOnComplete(rest.tail)
            }
          }))
      }
    }

    registerOnComplete(promises)

    promises(0).setValue(1)
    com.twitter.util.Await.result(promises.last)
    difference + benchmarkSuspend { ex.executor.shutdownNow }
  }

  def perf3TwitterUtil(n: Int, cores: Int): Long = {
    var ex: com.twitter.util.ExecutorServiceFuturePool = null
    val difference = benchmarkSuspend { ex = getTwitterUtilExecutor(cores) }

    val promises = (1 to n).map(_ => com.twitter.util.Promise[Int])

    def registerOnComplete(rest: Seq[com.twitter.util.Promise[Int]]) {
      val p1 = if (rest.size > 0) rest(0) else null
      val p2 = if (rest.size > 1) rest(1) else null
      if (p1 ne null) {
        /*
         * We cannot use respond for Twitter Util since there is no way of specifying the executor for the callback.
         */
        p1.transform(t => {
          ex({
            if (p2 ne null) {
              p2.setValue(1)
            }
          })
        })

        if (p2 ne null) {
          registerOnComplete(rest.tail)
        }
      }
    }

    registerOnComplete(promises)

    promises(0).setValue(1)
    com.twitter.util.Await.result(promises.last)
    difference + benchmarkSuspend { ex.executor.shutdownNow }
  }

  def perf1ScalaFP(n: Int, m: Int, k: Int, cores: Int): Long = {
    val counter = new Synchronizer(n * (m + k))
    var ex: Tuple2[ExecutorService, ExecutionContext] = null
    val difference = benchmarkSuspend { ex = getScalaFPExecutor(cores) }
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
          override def run(): Unit = {
            p.tryComplete(Success(1))
            counter.increment
          }
        })
      })
    })

    // get ps
    promises.foreach(p => Await.result(p.future, Duration.Inf))

    counter.await
    difference + benchmarkSuspend { executionService.shutdownNow }
  }

  def perf2ScalaFP(n: Int, cores: Int): Long = {
    var ex: Tuple2[ExecutorService, ExecutionContext] = null
    val difference = benchmarkSuspend { ex = getScalaFPExecutor(cores) }
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
    difference + benchmarkSuspend { executionService.shutdownNow }
  }

  def perf3ScalaFP(n: Int, cores: Int): Long = {
    var ex: Tuple2[ExecutorService, ExecutionContext] = null
    val difference = benchmarkSuspend { ex = getScalaFPExecutor(cores) }
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
    difference + benchmarkSuspend { executionService.shutdownNow }
  }

  def perf1Prim(n: Int, m: Int, k: Int, cores: Int, f: (Executor) => FP[Int]): Long = {
    val counter = new Synchronizer(n * (m + k))
    var ex: Executor = null
    val difference = benchmarkSuspend { ex = getPrimExecutor(cores) }
    val promises = (1 to n).map(_ => f(ex))

    promises.foreach(p => {
      1 to m foreach (_ => ex.submit(() => p.onComplete(t => counter.increment)))
      1 to k foreach (_ => ex.submit(() => {
        p.trySuccess(1)
        counter.increment
      }))
    })

    // get ps
    promises.foreach(p => p.getP)

    counter.await
    difference + benchmarkSuspend { ex.shutdown }
  }

  def perf2Prim(n: Int, cores: Int, f: (Executor) => FP[Int]): Long = {
    var ex: Executor = null
    val difference = benchmarkSuspend { ex = getPrimExecutor(cores) }
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
    promises.last.getP
    difference + benchmarkSuspend { ex.shutdown }
  }

  def perf3Prim(n: Int, cores: Int, f: (Executor) => FP[Int]): Long = {
    var ex: Executor = null
    val difference = benchmarkSuspend { ex = getPrimExecutor(cores) }
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

        registerOnComplete(rest.tail)
      }
    }

    registerOnComplete(promises)

    promises(0).trySuccess(1)
    promises.last.getP
    difference + benchmarkSuspend { ex.shutdown }
  }
}