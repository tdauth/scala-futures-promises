package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Promise

class StmFactory extends Factory {
  override def createPromise[T](ex: Executor): Promise[T] = new StmPromise[T](ex)
}