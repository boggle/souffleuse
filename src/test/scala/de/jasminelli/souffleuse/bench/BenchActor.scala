package de.jasminelli.souffleuse.bench

import actors.LoopingActor
import util.Latch

/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow<plantikow@zib.de> 
 *
 * Originally created by User: stepn Date: 19.02.2009 Time: 23:35:43
 */

trait BenchActor extends LoopingActor {

  protected val initialObl: Latch#Obligation
  
  @volatile protected var finalObl: Latch#Obligation = null

  override protected def onStartActing = {
    super.onStartActing
    initialObl.fullfill
  }

  override protected def onStopActing = {
    if (finalObl != null)
      finalObl.fullfill
    super.onStopActing
  }
}