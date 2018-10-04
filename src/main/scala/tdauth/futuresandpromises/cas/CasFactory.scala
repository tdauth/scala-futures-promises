package tdauth.futuresandpromises.cas

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Promise

class CasFactory extends Factory {
  override def createPromise[T](ex: Executor): Promise[T] = new CasPromise[T](ex)
}