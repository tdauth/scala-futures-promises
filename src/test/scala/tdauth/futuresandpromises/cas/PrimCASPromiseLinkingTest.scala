package tdauth.futuresandpromises.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractFPTest, FP, JavaExecutor}

class PrimCASPromiseLinkingTest extends AbstractFPTest {
  type FPLinkingType = PrimCASPromiseLinking[Int]

  override def getTestName: String = "PrimCASPromiseLinkingTest"
  override def getFP: FP[Int] = getFPPromiseLinking

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())

  "Link" should "link to another promise and be completed by it" in {
    val p0 = getFPPromiseLinking
    val p1 = getFPPromiseLinking
    p0.tryCompleteWith(p1)
    p1.isLinkTo(p0) shouldEqual true
    p1.trySuccess(10)
    p0.getP shouldEqual 10
    p1.getP shouldEqual 10
  }

  it should "link to another promise which links to another promise" in {
    val p0 = getFPPromiseLinking
    val p1 = getFPPromiseLinking
    val p2 = getFPPromiseLinking
    p0.tryCompleteWith(p1)
    p1.tryCompleteWith(p2)
    p1.isLinkTo(p0) shouldEqual true
    p2.isLinkTo(p1) shouldEqual true
    p2.trySuccess(10)
    p0.getP shouldEqual 10
    p1.getP shouldEqual 10
    p2.getP shouldEqual 10
  }

  /**
    * Creates 101 promises.
    * The final promise will be completed with 10. All other promises in between are only links.
    * [[FPLinkingType#getP]] will follow the links with the help of [[FPLinkingType#onComplete]] to returned the linked
    * result.
    */
  it should "create a chain of links" in {
    val n = 100
    val successfulP = getFPPromiseLinking

    def createChainElement(i: Int): FPLinkingType = {
      val p = getFPPromiseLinking
      val link = if (i == 0) successfulP else createChainElement(i - 1)
      p.tryCompleteWith(link)
      p
    }

    val p = createChainElement(n)
    successfulP.trySuccess(10)

    def assertChain(i: Int, p: FPLinkingType): Unit = {
      p.isLink shouldEqual true
      p.getP shouldEqual 10
      if (i > 0) assertChain(i - 1, p.getLinkTo)
    }

    assertChain(n, successfulP)
  }

  def getFPPromiseLinking: PrimCASPromiseLinking[Int] = new PrimCASPromiseLinking[Int](executor)
}
