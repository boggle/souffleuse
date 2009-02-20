package de.jasminelli.sofleuse.bench

import actors.LoopingActor
import util.Barrier

/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow<plantikow@zib.de> 
 *
 * Originally created by User: stepn Date: 19.02.2009 Time: 23:35:43
 */

trait BenchActor extends LoopingActor {

  protected val initialObl: Barrier#Obligation
  
  @volatile protected var finalObl: Barrier#Obligation = null

  override protected def onStartActing = {
    super.onStartActing
    initialObl.fullfill
  }

  override protected def onStopActing = {
    super.onStopActing
    if (finalObl != null)
      finalObl.fullfill
  }
}