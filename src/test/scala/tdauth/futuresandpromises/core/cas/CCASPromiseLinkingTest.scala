package tdauth.futuresandpromises.core.cas

import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import tdauth.futuresandpromises.core.FP
import tdauth.futuresandpromises.{AbstractFPTest, JavaExecutor}

import scala.collection.mutable.ListBuffer

class CCASPromiseLinkingTest extends AbstractFPTest {
  type FPLinkingType = CCASPromiseLinking[Int]

  override def getTestName: String = "CCASPromiseLinkingTest"
  override def getFP: FP[Int] = getFPPromiseLinking

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())

  "Link" should "link to another promise and be completed by it" in {
    val v = new AtomicBoolean(false)
    val p0 = getFPPromiseLinking

    p0.isLink() shouldEqual false
    p0.isLinkTo(p0) shouldEqual false
    p0.isListOfCallbacks() shouldEqual true
    p0.getNumberOfCallbacks() shouldEqual 0

    val p1 = getFPPromiseLinking
    p1.onComplete(_ => v.set(true))

    p1.isLink() shouldEqual false
    p1.isLinkTo(p0) shouldEqual false
    p1.isListOfCallbacks() shouldEqual true
    p1.getNumberOfCallbacks() shouldEqual 1

    p0.tryCompleteWith(p1)

    p0.isListOfCallbacks() shouldEqual true
    p0.getNumberOfCallbacks() shouldEqual 1
    p1.isLinkTo(p0) shouldEqual true

    p1.trySuccess(10)
    p0.getP shouldEqual 10
    p1.getP shouldEqual 10
    v.get() shouldEqual true
  }

  it should "link to another promise which links to another promise" in {
    val v = new AtomicInteger(0)
    val p0 = getFPPromiseLinking
    val p1 = getFPPromiseLinking
    p1.onComplete(_ => v.incrementAndGet())
    val p2 = getFPPromiseLinking
    p2.onComplete(_ => v.incrementAndGet())
    p0.tryCompleteWith(p1)
    p1.tryCompleteWith(p2)

    p0.isListOfCallbacks() shouldEqual true
    p0.getNumberOfCallbacks() shouldEqual 2
    p1.isLinkTo(p0) shouldEqual true
    // The compression makes sure that p2 does not link to p1 but directly to p2.
    p2.isLinkTo(p0) shouldEqual true

    p2.trySuccess(10)
    p0.getP shouldEqual 10
    p1.getP shouldEqual 10
    p2.getP shouldEqual 10
    v.get() shouldEqual 2
  }

  /**
    * Creates n + 1 promises.
    * n of them are links to p.
    * The final linked promise will be completed with 10 which completes p.
    * p should collect all the callbacks of all the n promises.
    */
  it should "create a chain of links" in {
    val n = 100
    val counter = new AtomicInteger(0)
    val links = ListBuffer[FPLinkingType]()

    def createChainElement(i: Int, current: FPLinkingType): FPLinkingType = {
      assert(i > 0)
      val p = getFPPromiseLinking
      p.onComplete(_ => counter.incrementAndGet())
      links += p
      current.tryCompleteWith(p)
      if (i == 1) p else createChainElement(i - 1, p)
    }

    // p should not be a link and collect all the callbacks.
    val p = getFPPromiseLinking
    val finalLink = createChainElement(n, p)

    links.size shouldEqual n
    p.isListOfCallbacks() shouldEqual true
    p.getNumberOfCallbacks() shouldEqual n
    finalLink.isLink() shouldEqual true
    finalLink.isLinkTo(p) shouldEqual true

    def assertUncompletedChain(links: ListBuffer[FPLinkingType]): Unit = {
      if (!links.isEmpty) {
        val l = links.head
        l.isLink shouldEqual true
        // The compression lets all links directly link to p.
        l.isLinkTo(p) shouldEqual true
        if (links.tail ne null) assertUncompletedChain(links.tail)
      }
    }

    assertUncompletedChain(links)

    finalLink.trySuccess(10)

    p.isLink() shouldEqual false
    p.isReady shouldEqual true
    p.getP shouldEqual 10
    counter.get() shouldEqual n

    def assertCompletedChain(links: ListBuffer[FPLinkingType]): Unit = {
      if (!links.isEmpty) {
        val l = links.head
        l.isLink shouldEqual true
        l.isLinkTo(p) shouldEqual true
        l.isReady shouldEqual true
        l.getP shouldEqual 10
        if (links.tail ne null) assertCompletedChain(links.tail)
      }
    }

    assertCompletedChain(links)
  }

  it should "produce incorrect behaviour by calling all callbacks although completing only one linked promise" in {
    val counter = new AtomicInteger(0)
    val p = getFPPromiseLinking
    val f = getFPPromiseLinking
    f.onComplete(_ => counter.incrementAndGet())
    val g = getFPPromiseLinking
    g.onComplete(_ => counter.incrementAndGet())
    p.tryCompleteWith(f)
    p.tryCompleteWith(g)

    p.isListOfCallbacks() shouldEqual true
    p.getNumberOfCallbacks() shouldEqual 2
    f.isLinkTo(p) shouldEqual true
    g.isLinkTo(p) shouldEqual true

    g.trySuccess(10)

    g.getP shouldEqual 10
    f.getP shouldEqual 10
    p.getP shouldEqual 10

    counter.get() shouldEqual 2
  }

  def getFPPromiseLinking: FPLinkingType = new FPLinkingType(executor)
}
