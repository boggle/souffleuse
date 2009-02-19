package de.jasminelli.sofleuse.bench

import java.util.concurrent.atomic.AtomicInteger

/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow <stefan.plantikow@googlemail.com> 
 *
 * Originally created by User: stepn Date: 19.02.2009 Time: 18:47:26
 */
sealed class Verificator {
  def generateList(n: Int): List[Byte] =
    (for (_ <- 0.until(n)) yield (Math.random*256.0).byteValue).toList

  def stagesPassed: Int = 0

  def incrStagesPassed: Unit = ()
  def resetStagesPassed: Unit = ()

  def testStagesPassed(rqStages: Int): Boolean = true
}


final class PlainVerificator extends Verificator ;

final class CountingVerificator extends Verificator {
  private var _rqStageCount: AtomicInteger = new AtomicInteger(0)

  override def stagesPassed = _rqStageCount.get
  override def incrStagesPassed = _rqStageCount.incrementAndGet
  override def resetStagesPassed = _rqStageCount.set(0)

  override def testStagesPassed(rqStages: Int): Boolean = rqStages == stagesPassed
}


