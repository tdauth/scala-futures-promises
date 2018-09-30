package tdauth.futuresandpromises.twitter

import com.twitter.util.FuturePool

import java.util.concurrent.ForkJoinPool

import tdauth.futuresandpromises.Executor

// TODO #18 Use FuturePool/ExecutorServiceFuturePool
class TwitterExecutor(val e: java.util.concurrent.Executor) extends Executor {
  def this() = this(ForkJoinPool.commonPool())

  override def submit(f: () => Unit): Unit = e.execute(new Runnable {
    override def run: Unit = f.apply()
  })
}

object TwitterExecutor {
  val global = new TwitterExecutor
}