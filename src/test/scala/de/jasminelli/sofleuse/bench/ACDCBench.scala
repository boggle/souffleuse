package de.jasminelli.sofleuse.bench

import _root_.de.jasminelli.sofleuse.core.Play
import de.jasminelli.sofleuse.actors._

import util.Barrier

/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow<plantikow@zib.de> 
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:10:14
 */

class ACDCBench(params: BenchParams) extends RpcBench(params) {
  @volatile var first: BenchStage = null
  @volatile var finalObl: Barrier#Obligation = null


  class BenchStage(obl: Barrier#Obligation, val next: BenchStage) extends StageActor {

    override def onStartActing = {
      super.onStartActing;
      obl.fullfill;
    }

    override protected def onUnknownMessage(msg: Any):Unit = {
      msg match {
        case (obl: Barrier#Obligation) => { shutdownAfterScene; finalObl = obl }
        case _ => throw new StageActor.UnknownMessageException
      }
    }

    override def onStopActing = {
      super.onStopActing;
      first = null
      if (finalObl != null) { finalObl.fullfill; finalObl = null }
    }

    start
  }


  def startup: Barrier = {
    val bar = new Barrier('startup)
    first = 0.until(params.numStages).foldRight[BenchStage](null)
              { (id, next) => new BenchStage(bar.newObligation, next) }
    bar
  }


  def sendRequest(rqStage: BenchStage, cont: (BenchStage => Unit)): Unit =
    (for (stage <- Play.goto(rqStage);
         _ <- Play.compute {
                if (stage.next == null) cont(rqStage) else sendRequest(stage.next, cont)
         })
    yield ()).respond { _ => () }


  override def request(obl: Barrier#Obligation) = sendRequest(first, { _  => obl.fullfill })


  def shutdown: Barrier = {
    var bar = new Barrier('shutdown)
    var cur = first
    while (cur != null) {
      cur ! bar.newObligation
      cur = cur.next
    }
    first = null
    bar
  }
}