package tdauth.futuresandpromises

/**
  * Abstract type for the current value stored by a future/promise.
  */
trait CallbackEntry

/**
  * Connects two entries together.
  * This is required by promise linking when appending a number of callbacks.
  * It is similar to the internal DefaultPromise.ManyCallbacks class of the Scala 2.13.xx standard library.
  */
case class ParentCallbackEntry(final val left: CallbackEntry, final val right: CallbackEntry) extends CallbackEntry

/**
  * Indicates that there is no callback.
  */
case class EmptyCallbackEntry() extends CallbackEntry

object CallbackEntry {
  val Noop = EmptyCallbackEntry()
}
