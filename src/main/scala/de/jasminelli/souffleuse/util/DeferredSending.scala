package de.jasminelli.souffleuse.util

import scala.actors.{Actor, Channel, OutputChannel}
import actors.LoopingActor

/**
 * @author Stefan Plantikow <stefan.plantikow@googlemail.com>
 */

trait DeferredSending {
  self: Actor =>

  final private case class Send(dest: OutputChannel[Any], msg: Any) ;

  // TODO: Send to array, will destroy FIFO!

  private object SendingActor extends LoopingActor {
    var parent: Actor = null.asInstanceOf[Actor]

    override def onStartActing = {
      super.onStartActing
      link(parent)
    }

    override def mainLoopBody = {
      receiveWithin(1) {
        case (box: Send) =>
          box.dest.forward(box.msg)
        case (x: Any) => ()
      }
    }
  }

  def sendDeferred(dest: OutputChannel[Any], msg: Any) = SendingActor.!(Send(dest, msg))

  def startDeferredSending(parent: Actor) = {
    SendingActor.parent = parent
    SendingActor.start
  }


  def stopDeferredSending = {
    SendingActor.shutdownAfterScene
  }
}