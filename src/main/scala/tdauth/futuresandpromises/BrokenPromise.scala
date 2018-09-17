package tdauth.futuresandpromises

/**
 * This exception should be thrown if a promise is deleted before it completes its corresponding future.
 * In Scala, this exception will never be thrown since there is no explicit delete operation and the garbage collection won't release a promise
 * before its references won't exist anymore.
 */
class BrokenPromise extends Exception {
}