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

class ACDCBench(params: BenchParams, verific: Verificator) extends RpcBench(params, verific) {
  override type ActorType = BenchStage

  class BenchStage(obl: Barrier#Obligation, val next: BenchStage, val verify: Byte)
          extends StageActor with BenchActor {

    override protected val initialObl = obl

    override protected def onUnknownMessage(msg: Any):Unit = {
      msg match {
        case (shutdownObl: Barrier#Obligation) => { shutdownAfterScene; finalObl = shutdownObl }
        case _ => throw new StageActor.UnknownMessageException
      }
    }

    start
  }

  def mkStage(obl: Barrier#Obligation, next: ActorType, verifyByte: Byte) =
    new BenchStage(obl, next, verifyByte)

  def nextStage(stage: BenchStage) = stage.next
  
  def sendRequest(count: Int, list: List[Byte], rqStage: BenchStage, cont: (Int => BenchStage => Unit)): Unit =
    (for (stage <- Play.goto(rqStage);
         _ <- Play.compute {
                assert(list.head == rqStage.verify, "Verification failed, stages not passed correctly!")
                sleep(params.workDur)
                verific.incrStagesPassed
                if (stage.next == null) cont(count)(stage)
                else sendRequest(count + 1, list.tail, stage.next, cont)
         })
    yield ()).respond { _ => () }


  def doParRequests(numRqs: Int) = {
    // Send out bulk of requests
    for (r <- 0.until(numRqs)) {
      val chan: Channel[Any] = new Channel[Any](Actor.self)
      sendRequest(1, verifyList, first, { (count: Int) => { _  => chan ! count;  } })
    }

    // Wait for results from all / global completion of partition
    var outstanding = numRqs
    while (outstanding > 0)
      Actor.self.receive {
        case !(ch: Channel[Any], count: Int) => {
          if (count != params.numStages)
            throw new IllegalStateException("Unexpected or wrong stage count: " + count)
          outstanding = outstanding - 1
        }
        case _ => throw new IllegalStateException("Unexpected or wrong result message")
      }
  }
}