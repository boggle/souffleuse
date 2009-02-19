package de.jasminelli.sofleuse.bench


import de.jasminelli.sofleuse.core.Play
import de.jasminelli.sofleuse.actors._
import scala.actors._
import scala.actors.Actor._
import util.Barrier

/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow<plantikow@zib.de> 
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:10:14
 */

class ACDCBench(params: BenchParams) extends RpcBench(params) {
  var first: BenchStage = null


  class BenchStage(obl: Barrier#Obligation, val next: BenchStage) extends StageActor {
    var finalObl: Barrier#Obligation = null

    override def onStartActing = {
      super.onStartActing;
      obl.fullfill;
    }

    override protected def onUnknownMessage(msg: Any):Unit = {
      msg match {
        case (shutdownObl: Barrier#Obligation) => { shutdownAfterScene; finalObl = shutdownObl }
        case _ => throw new StageActor.UnknownMessageException
      }
    }

    override def onStopActing = {
      super.onStopActing;
      first = null
      if (finalObl != null) {
        finalObl.fullfill;
      }
    }

    start
  }


  def startup: Barrier = {
    val bar = new Barrier('startup, params.numStages)
    first = 0.until(params.numStages).foldRight[BenchStage](null)
              { (id, next) => new BenchStage(bar.newObligation, next) }
    bar
  }


  def sendRequest(count: Int, rqStage: BenchStage, cont: (Int => BenchStage => Unit)): Unit =
    (for (stage <- Play.goto(rqStage);
         _ <- Play.compute {
                sleep(params.workDur)
                if (stage.next == null) cont(count)(stage)
                else sendRequest(count + 1, stage.next, cont)
         })
    yield ()).respond { _ => () }


  override def request(obl: Barrier#Obligation): Int = {
    var chan: Channel[Any] = new Channel[Any](Actor.self)
    sendRequest(1, first, { (count: Int) => { _  => chan ! count;  } })
    chan.receive {
      case (count: Int) => return count 
      case (x: Any) => throw new IllegalStateException("Unexpected or wrong result")
    }
  }


  def parRequests(obl: Barrier#Obligation, numRqs: Int) = {
    // Send out bulk of requests
    for (r <- 0.until(numRqs)) {
      val chan: Channel[Any] = new Channel[Any](Actor.self)
      sendRequest(1, first, { (count: Int) => { _  => chan ! count;  } })
    }

    // Wait for results from all / global completion of partition
    var outstanding = numRqs
    while (outstanding > 0)
      Actor.self.receive {
        case !(ch: Channel[Any], count: Int) => {
          outstanding = outstanding - 1
        }
        case _ => throw new IllegalStateException("Unexpected or wrong result message")
      }

    obl.fullfill
  }


  def shutdown: Barrier = {
    var stages: List[BenchStage] = List()
    var cur = first
    while (cur != null) {
      stages = List(cur) ++ stages
      cur = cur.next
    }

    val bar = new Barrier('shutdown, stages.length)
    for (stage <- stages)
      stage ! bar.newObligation
    bar
  }
}