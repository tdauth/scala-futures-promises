package tdauth.futuresandpromises.twitter

import com.twitter.util.FuturePool
import tdauth.futuresandpromises.Executor

class TwitterExecutor(val futurePool: FuturePool) extends Executor {

  def this() = this(FuturePool.unboundedPool)

  override def submit(f: () => Unit): Unit = futurePool(f())
}

object TwitterExecutor {
  val global = new TwitterExecutor
}