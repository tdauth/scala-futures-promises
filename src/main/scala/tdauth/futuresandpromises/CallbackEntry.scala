package tdauth.futuresandpromises

/**
  * Abstract type for the current value stored by a future/promise.
  */
sealed trait CallbackEntry

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

/**
  * Single backwards linked list of callbacks.
  * When appending a callback, it will only be reversed link and the current link will be replaced by the new element.
  * This should improve the performance on appending elements compared to storing the whole list.
  * This does also mean that when the callbacks are called, they will be called in reverse order.
  */
case class LinkedCallbackEntry[T](final val c: (Try[T]) => Unit, final val prev: CallbackEntry) extends CallbackEntry

/**
  * If there is no link to previous callback entry yet, only the callback has to be stored.
  */
case class SingleCallbackEntry[T](final val c: (Try[T]) => Unit) extends CallbackEntry

object CallbackEntry {
  val Noop = EmptyCallbackEntry()
}
