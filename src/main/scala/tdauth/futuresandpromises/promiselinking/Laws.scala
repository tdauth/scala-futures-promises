package tdauth.futuresandpromises.promiselinking

object Laws {

  /**
   * The law of promise linking:
   *
   * ```
   * p = newP
   * f1 onComplete (x => p.tryCompleteWith(f2))
   * return p
   *
   * equals
   *
   * f2 replaces p when f1 has been completed
   * ```
   * The law says that f2 can be released by the garbage collection when it equals p, even before it has been completed!
   * This law works since x is ignored.
   * This is the law which promise linking is based on.
   *
   *
   * More generalized:
   * ```
   * p.tryCompleteWith(f2)
   *
   * equals
   *
   * f2 replaces p
   * ```
   * TODO Is this what become does in Twitter util and promise linking in Scala FP?
   * Scala FP uses a Transform class which uses linking for transformWith, flatMap and recoverMap manually and only in the latest version 2.13.xx
   *
   *
   * Twitter util uses a Transform class which calls:
   * `p.become(f2)`
   * `become` calls `f2.link(p)`
   * `link` replaces the callback list in `f2` by `p` and appends the callback list to `p`.
   * This means that actually f2 is replaced by p?!!!!!
   * f2 can be released.
   *
   * If there is another become called on f2, it is compressed and the link is directly done to p.
   * The root is the most left promise which is NO link.
   *
   *
   *
   *
   *
   *
   * Twitter util does also provide `transformedBy` which requires a custom `FutureTransformer`.
   * This transformer requires the methods to be specified manually.
   * `flatMap` is based on `transform`, so it uses the same technique.
   * There is no `recoverWith`.
   * There is a `Promise. updateIfEmpty` but no `tryCompleteWith`. Instead, we only have the intern `class Transformer`.
   *
   * TODO Why am I allowed to call become on different futures which could be completed with different values?!
   * Test the following:
   * p0 become(f1)
   * p0 become(f2)
   * will all callbacks be appended to p0?
   * Isn't there a race for which future completes p0 first?
   */
}