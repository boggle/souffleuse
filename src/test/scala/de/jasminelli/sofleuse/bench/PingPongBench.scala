package de.jasminelli.sofleuse.bench

import de.jasminelli.sofleuse.actors._
import scala.actors._
import scala.actors.Actor._
import scala.Console
import util.Barrier
import scala.collection.immutable._
/**
 * @author Stefan Plantikow<plantikow@zib.de>
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:10:47
 */
class PingPongBench(params: BenchParams) extends RpcBench(params) {
  private var stages: Array[Actor] = null

  class BenchStage(obl: Barrier#Obligation, sid: Int) extends LoopingActor {
    @volatile var finalObl: Barrier#Obligation = null

    override def onStartActing = {
      super.onStartActing;
      obl.fullfill;
    }

    def mainLoopBody =  receive {
        case (msg: Symbol) => { sleep(params.workDur); Actor.reply(sid + 1) }
        case (someObl: Barrier#Obligation) => { shutdownAfterScene; finalObl = someObl }
    }

    override def onStopActing = {
      super.onStopActing; 
      if (finalObl != null) finalObl.fullfill
    }
  }

  def startup: Barrier = {
    val bar = new Barrier('startup, params.numStages)
    stages = new Array[Actor](params.numStages)
    for (sid <- 0.until(params.numStages))
      stages(sid) = new BenchStage(bar.newObligation, sid).start
    bar
  }

  override def request(obl: Barrier#Obligation): Int = {
    var result: Int = 0
    for (sid <- 0.until(params.numStages)) {
      val nextSid = (stages(sid) !? 'compute).asInstanceOf[Int]
      assert (nextSid == sid + 1)
      result = result + 1
    }
    result
  }

  def parRequests(obl: Barrier#Obligation, numRqs: Int): Unit = {
    // Send out
    var channels: Set[Channel[Any]] = Set.empty
    for (r <- 0.until(numRqs)) {
      val chan = new Channel[Any](Actor.self)
      stages(0).send('compute, chan)
      channels = channels + chan
    }


    // Collect and dispatch until done
    while (! channels.isEmpty) {
      Actor.self.receive {
        case !(ch: Channel[Any], nextSid: Int) =>  {
          channels = channels - ch
          if (nextSid < params.numStages) {
            val chan = new Channel[Any](Actor.self)
            stages(nextSid).send('compute, chan)
            channels = channels + chan
          }
        }
        case _ => throw new IllegalStateException("Unexpected or wrong result message")
      }
    }

    obl.fullfill
  }

  def shutdown = {
    val bar = new Barrier('shutdown, params.numStages)
    for (sid <- 0.until(params.numStages))
      stages(sid) ! bar.newObligation
    stages = null
    bar
  }
}
