package tdauth.futuresandpromises.stm

import scala.concurrent.stm._
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Try

/**
 * The shared state of futures and promises which contains the result value, callback and executor reference.
 * This class is called Core in Folly.
 *
 * The operations are implemented with the help of STM.
 */
class StmSharedState[T] {
  type CallbackType = (Try[T]) => Unit

  /**
   * This callback is called by {@link #tryComplete} when the result of the shared state is set.
   */
  private var callback = Ref[CallbackType](null)
  /**
   * The callback is submitted to this executor when being called.
   */
  private var ex = Ref[Executor](null)
  private var t = Ref(new StmTry[T])

  def sync: Unit = {
    atomic { implicit txn =>
      if (t().hasValue || t().hasException) retry
    }
  }

  def get(): T = {
    /*
     * Blocking with STM is done with retry: https://nbronson.github.io/scala-stm/modular_blocking.html
     */
    atomic { implicit txn =>
      if (t().hasValue || t().hasException) retry

      t().get()
    }
  }

  def isReady: Boolean = {
    atomic { implicit txn =>
      return t().hasValue || t().hasException
    }
  }

  def tryComplete(v: Try[T]): Boolean = {
    val stmTry = v.asInstanceOf[StmTry[T]]

    atomic { implicit txn =>
      t() = stmTry
      val c = callback()
      val e = ex()

      if (e != null) {
        e.submit(() => {
          c(v)
        })
      } else {
        c(v)
      }

      return true
    }
  }

  def setCallback(callback: CallbackType) = {
    atomic {
      implicit txn =>
        this.callback() = callback
    }
  }

  def getExecutor(): Executor = {
    atomic {
      implicit txn =>
        return this.ex()
    }
  }

  def setExecutor(ex: Executor) = {
    atomic {
      implicit txn =>
        this.ex() = ex
    }
  }
}