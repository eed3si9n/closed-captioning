package com.eed3si9n.cc

import akka.actor._
import akka.util.duration._
import akka.dispatch.Future
import unfiltered.netty.websockets.{WebSocket, Text, Binary}
import collection.mutable
import java.nio.channels.ClosedChannelException

case class SocketActor(socket: WebSocket) extends Actor {
  def receive = {
    case Text(txt)   =>
      try {
        socket.send(txt)  
      }
      catch {
        case e: ClosedChannelException => self.stop()
        case e: Throwable => throw e
      }
    case Binary(buf) =>
      try {
        socket.send(buf)  
      }
      catch {
        case e: ClosedChannelException => self.stop()
        case e: Throwable => throw e
      }
    case _      => // log.info("received unknown message")
  }
}

case class AddSocket(socket: ActorRef)
case class RemoveSocket(socket: ActorRef)
trait BroadcastActor {
  val sockets = mutable.Set[ActorRef]()
  val addRemoveSocket: Actor.Receive = {
    case AddSocket(socket) => sockets += socket
    case RemoveSocket(socket) => sockets -= socket
  }
  def broadcast(txt: Text) {
    sockets foreach { socket =>
      socket ! txt
    }
  }
}

case class Grep(q: String, count: Int)
case class TweetActor() extends Actor with BroadcastActor {
  val twt = Twt()
  val seen = mutable.Set[BigDecimal]()

  def receive = addRemoveSocket orElse {
    case Grep(q, count) =>
      twt.auth_token map { t =>
        val statuses = twt.grep(q, count)(t)
        statuses map { s =>
          if (!seen.contains(s.id)) {
            seen += s.id
            broadcast(Text(s.screenName + "|" + s.status))
          } // if
        }
      } getOrElse {
        println("no access token!")
      }
  }
}

case object StartTimer {}
case class TimerActor(q: String, count: Int, intervalMSec: Long, tweet: ActorRef) extends Actor {
  def receive = {
    case StartTimer =>
      while (true) {
        tweet ! Grep(q, count)
        Thread.sleep(intervalMSec)
      }
  }
}

case class IrcActor(encoding: String, nickName: String, userName: String,
  hostName: String, port: Int, channel: String) extends Actor with irc.IrcClient with BroadcastActor {
  joinChannel(channel)
  def receive = addRemoveSocket orElse {
    case _      => // log.info("received unknown message")
  }
  override def onMessage(message: irc.Message) {
    broadcast(Text(message.nickName + "|" + message.text))
  }
  override def postStop() {
    disconnect
  }
}
