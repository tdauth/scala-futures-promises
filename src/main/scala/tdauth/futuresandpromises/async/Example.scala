package tdauth.futuresandpromises.async

import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.{ async, await }

object Example {

  val future = async {
    val f1 = async { true }
    val f2 = async { 42 }
    if (await(f1)) await(f2) else 0
  }
}