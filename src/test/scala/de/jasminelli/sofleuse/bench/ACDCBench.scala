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

  class BenchStage(obl: Barrier#Obligation,
                  val next: BenchStage, val nextId: Int, val verifyList: Array[Byte])
          extends StageActor with BenchActor {

    override protected val initialObl = obl

    protected[ACDCBench] val verifyByte = verifyList(nextId - 1)

    def verify(idx: Int): Boolean = verifyList(idx) == verifyByte

    override protected def onUnknownMessage(msg: Any):Unit = {
      msg match {
        case (shutdownObl: Barrier#Obligation) => { shutdownAfterScene; finalObl = shutdownObl }
        case _ => throw new StageActor.UnknownMessageException
      }
    }

    start
  }

  def mkStage(obl: Barrier#Obligation, next: BenchStage, nextId: Int, verifyList: Array[Byte]) =
    new BenchStage(obl, next, nextId, verifyList)

  def nextStage(stage: BenchStage) = stage.next
  
  def sendRequest(count: Int, rqStage: BenchStage, cont: (Int, BenchStage) => Unit): Unit =
    (for (stage <- Play.goto(rqStage);
         _ <- Play.compute {
                sleep(params.workDur)
                if (verific.stageBasedVerify)
                  assert(rqStage.verify(count), "Verification failed, stages not passed correctly!")
                verific.incrStagesPassed
                if (stage.next == null) cont(count + 1, stage)
                else sendRequest(count + 1, stage.next, cont)
         })
    yield ()).respond { _ => () }


  def doParRequests(numRqs: Int) = {
    // Send out bulk of requests
    val selfActor = Actor.self

    val replier = if (params.replyActors) {
      { (count: Int, stage: BenchStage) => Actor.actor { selfActor.!(count); Actor.self.exit }; () }     
    }
    else { (count: Int, stage: BenchStage) => selfActor.!(count) }

    for (r <- 0.until(numRqs))
      sendRequest(0, first, replier)

    // Wait for results from all / global completion of partition
    var outstanding = numRqs
    while (outstanding > 0)
      selfActor.receive {
        case (count: Int) => {
          if (count != params.numStages)
            throw new IllegalStateException("Unexpected or wrong stage count: " + count)
          outstanding = outstanding - 1
        }
        case _ => throw new IllegalStateException("Unexpected or wrong result message")
      }
  }
}