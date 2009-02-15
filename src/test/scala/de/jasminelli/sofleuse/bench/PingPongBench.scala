package de.jasminelli.sofleuse.bench

import _root_.scala.actors.{Actor, Channel}
import actors.LoopingActor
import util.Barrier

/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow<plantikow@zib.de> 
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:10:47
 */
class PingPongBench(params: BenchParams) extends RpcBench(params) {
  var stages: Array[Actor] = null

  class BenchStage(obl: Barrier#Obligation) extends LoopingActor {
    @volatile var finalObl: Barrier#Obligation = null

    override def onStartActing = {
      super.onStartActing;
      obl.fullfill;
    }

    def mainLoopBody =  receive {
        case (msg: Symbol) => Actor.reply(1)
        case (someObl: Barrier#Obligation) => { shutdownAfterScene; finalObl = someObl }
    }

    override def onStopActing = {
      super.onStopActing; stages = null;
      if (finalObl != null) finalObl.fullfill
    }
  }

  def startup: Barrier = {
    val bar = new Barrier('startup)
    stages = new Array[Actor](params.numStages)
    0.until(params.numStages).foreach
              { sid => stages(sid) = new BenchStage(bar.newObligation).start }
    bar
  }

  override def request(obl: Barrier#Obligation) = {
    0.until(params.numStages).foreach { sid => stages(sid) !? 'compute }
    obl.fullfill
  }

  def shutdown = {
    val bar = new Barrier('shutdown)
    0.until(params.numStages).foreach { sid => stages(sid) ! bar.newObligation }
    stages = null
    bar
  }
}
