package com.eed3si9n.cc

import akka.actor._
import akka.util.duration._
import akka.dispatch.Future
import unfiltered.netty.websockets.{WebSocket, Text, Binary}
import collection.mutable
import java.nio.channels.ClosedChannelException
import grizzled.slf4j._

// http://docs.jboss.org/netty/3.2/api/org/jboss/netty/channel/Channel.html
case class SocketActor(socket: WebSocket) extends Actor {
  def receive = {
    case Text(txt)   =>
      try {
        if (socket.channel.isOpen) socket.send(txt)
        else self.stop()  
      }
      catch {
        case e: ClosedChannelException => self.stop()
        case e: Throwable => throw e
      }
    case Binary(buf) =>
      try {
        if (socket.channel.isOpen) socket.send(buf)
        else self.stop()  
      }
      catch {
        case e: ClosedChannelException => self.stop()
      }
    case x => println("received unknown message" + x.toString)
  }
}

case class Notice(source: String, screenName: String, profileImageUrl: String, status: String) {
  def toText: Text =
    Text("%s|%s|%s|%s".format(source, screenName, profileImageUrl, status))
}
case class BroadcastActor() extends Actor {
  def sockets = Actor.registry.actorsFor[SocketActor] filter {_.isRunning}
  def receive = {
    case notice: Notice =>
      sockets foreach { socket =>
        try {
          socket ! notice.toText
        }
        catch {
          case e: ActorInitializationException => e.printStackTrace
        }
      }
    case x => println("received unknown message" + x.toString)
  }
}

case class Grep(q: String, count: Int)
case class TweetActor(bcast: ActorRef) extends Actor {
  val twt = Twt()
  val seen = mutable.Set[BigDecimal]()

  def receive = {
    case Grep(q, count) =>
      twt.auth_token map { t =>
        val statuses = twt.grep(q, count)(t)
        statuses map { s =>
          // skip the retweets and already seen ones
          if (!s.status.startsWith("RT ") &&  !seen.contains(s.id)) {
            seen += s.id
            bcast ! Notice("twitter", s.screenName, s.profileImageUrl, s.status)
          } // if
        }
      } getOrElse {
        println("no access token!")
      }
    case x => println("received unknown message" + x.toString)
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
  hostName: String, port: Int, useSsl: Boolean, channel: String, bcast: ActorRef) extends Actor with irc.IrcClient {
  joinChannel(channel)
  def receive = {
    case x => println("received unknown message" + x.toString)
  }
  override def onMessage(message: irc.Message) {
    bcast ! Notice("irc", message.nickName, "_", message.text)
  }
  override def postStop() {
    disconnect
  }
}
