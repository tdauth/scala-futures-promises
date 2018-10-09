package tdauth.futuresandpromises

import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import org.scalameter.api.Bench
import org.scalameter.api.Gen
import java.util.concurrent.Executors
import tdauth.futuresandpromises.cas.PrimCAS
import tdauth.futuresandpromises.mvar.PrimMVar
import tdauth.futuresandpromises.stm.PrimSTM
import scala.concurrent.Promise
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import tdauth.futuresandpromises.cas.PrimCAS
import scala.util.Success
import org.scalameter.reporting.LoggingReporter

object FPPerformanceTest extends Bench[Double] {
  val NUMBER_OF_NEW_PS: Gen[Int] = Gen.range("size")(1000, 10000, 1000)
  val NUMBER_OF_TRY_COMPLETE: Gen[Int] = Gen.range("size")(1000, 10000, 1000)
  val NUMBER_OF_ON_COMPLETES: Gen[Int] = Gen.range("size")(10, 100, 10)
  val NUMBER_OF_FOLLOWED_BYS: Gen[Int] = Gen.range("size")(100, 1000, 100)
  val executionService = Executors.newSingleThreadExecutor()
  val ex = new JavaExecutor(executionService)
  val executionContext = ExecutionContext.fromExecutorService(executionService)

  lazy val executor = SeparateJvmsExecutor(
    new Executor.Warmer.Default,
    Aggregator.min,
    new Measurer.Default)
  lazy val measurer = new Measurer.Default
  lazy val reporter = new LoggingReporter[Double]
  lazy val persistor = Persistor.None

  // TODO Test newP
  // TODO Test tryComplete

  performance of "ScalaFP" in {
    measure method "newP" in {
      using(NUMBER_OF_NEW_PS) in {
        r => for (i <- 0 to r) yield Promise.apply[Int]
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield Promise.apply[Int]
            promises.foreach(p => p.tryComplete(Success(10)))
          }
      }
    }

    // TODO Takes forever! Number of onCompletes does not matter. Maybe one thread executor?
    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = Promise.apply[Int]
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
            val p = Promise.apply[Int]
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
        r => for (i <- 0 to r) yield new PrimCAS[Int](ex)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimCAS[Int](ex)
            promises.foreach(p => p.tryComplete(new Try[Int](10)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = new PrimCAS[Int](ex)
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
            val p = new PrimCAS[Int](ex)
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
        r => for (i <- 0 to r) yield new PrimMVar[Int](ex)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimMVar[Int](ex)
            promises.foreach(p => p.tryComplete(new Try[Int](10)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = new PrimMVar[Int](ex)
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
            val p = new PrimMVar[Int](ex)
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
        r => for (i <- 0 to r) yield new PrimSTM[Int](ex)
      }
    }

    measure method "tryComplete" in {
      using(NUMBER_OF_TRY_COMPLETE) in {
        r =>
          {
            val promises = for (i <- 0 to r) yield new PrimSTM[Int](ex)
            promises.foreach(p => p.tryComplete(new Try[Int](10)))
          }
      }
    }

    measure method "onComplete" in {
      using(NUMBER_OF_ON_COMPLETES) in {
        r =>
          {
            val p = new PrimSTM[Int](ex)
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
            val p = new PrimSTM[Int](ex)
            val nested = nestedFollowedBy(p, r)
            p.trySuccess(10)
            nested.get
          }
      }
    }
  }

  @tailrec def registerOnCompletesScalaFP(f: Future[Int], i: Int): Unit = {
    f.onComplete(() => _)(executionContext)
    if (i > 0) registerOnCompletesScalaFP(f, i - 1)
  }

  @tailrec def nestedMap(f: Future[Int], i: Int): Future[Int] = {
    val p = f.map(t => t + 1)(executionContext)
    if (i > 0) nestedMap(p, i - 1) else p
  }

  @tailrec def registerOnCompletes(f: FP[Int], i: Int): Unit = {
    f.onComplete(() => _)
    if (i > 0) registerOnCompletes(f, i - 1)
  }

  @tailrec def nestedFollowedBy(f: FP[Int], i: Int): FP[Int] = {
    val p = f.followedBy(t => t + 1)
    if (i > 0) nestedFollowedBy(p, i - 1) else p
  }
}