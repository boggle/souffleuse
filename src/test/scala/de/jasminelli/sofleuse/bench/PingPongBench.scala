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

  class BenchStage(obl: Barrier#Obligation, sid: Int, verify: Byte) extends LoopingActor {
    @volatile var finalObl: Barrier#Obligation = null

    override def onStartActing = {
      super.onStartActing;
      obl.fullfill;
    }

    def mainLoopBody =  receive {
        case (lst: List[Byte]) => {
          sleep(params.workDur);
          assert(lst.head == verify, "Verification failed, stages not passed correctly!")
          incrStagesPassed
          Actor.reply((sid + 1, lst.tail)) 
        }
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
    var list: List[Byte] = verifyList
    for (sid <- 0.until(params.numStages)) {
      stages(sid) = new BenchStage(bar.newObligation, sid, list.head).start
      list = list.tail
    }
    bar
  }

  override def request(obl: Barrier#Obligation): Int = {
    var rqResult: Int = 0
    var list = verifyList
    for (sid <- 0.until(params.numStages)) {
      val result: (Int, List[Byte]) = (stages(sid) !? list).asInstanceOf[(Int, List[Byte])]
      val nextSid = result._1
      assert(nextSid == sid + 1)
      rqResult = rqResult + 1
      list = result._2
    }
    assert(list.isEmpty)
    rqResult
  }

  def parRequests(obl: Barrier#Obligation, numRqs: Int): Unit = {
    // Send out
    var channels: Set[Channel[Any]] = Set.empty
    for (r <- 0.until(numRqs)) {
      val chan = new Channel[Any](Actor.self)
      stages(0).send(verifyList, chan)
      channels = channels + chan
    }


    // Collect and dispatch until done
    while (! channels.isEmpty) {
      Actor.self.receive {
        case !(ch: Channel[Any], result: (Int, List[Byte])) =>  {
          val nextSid = result._1
          channels = channels - ch
          if (nextSid < params.numStages) {
            val chan = new Channel[Any](Actor.self)
            stages(nextSid).send(result._2, chan)
            channels = channels + chan
          }
          else
            assert(result._2.isEmpty)
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
