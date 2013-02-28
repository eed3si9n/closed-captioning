package com.eed3si9n.cc

object App {
  import unfiltered.netty.websockets._
  import unfiltered.util._
  import unfiltered.response.ResponseString
  import akka.actor._
  import akka.actor.Actor._
  import akka.util.duration._
  import com.typesafe.config.ConfigFactory
  import grizzled.slf4j._

  implicit val timeout = Timeout(100 millisecond)

  def socketActorName(s: WebSocket): String = "socketactor" + s.channel.getId.toString
    
  def main(args: Array[String]) {
    val logger = Logger("App")
    val setting = new CCSetting(ConfigFactory.load())

    logger.info("start")

    val broadcastactor = actorOf(BroadcastActor()).start()
    val tweetactor = actorOf(TweetActor(broadcastactor)).start()
    val timeractor = actorOf(TimerActor(
      setting.twitter.searchQuery, setting.twitter.count,
      setting.twitter.interval, tweetactor)).start()
    val ircactor = actorOf(IrcActor(
      setting.irc.encoding, setting.irc.nickname, setting.irc.username,
      setting.irc.hostname, setting.irc.port, setting.irc.use_ssl,
      setting.irc.channel,
      broadcastactor
    )).start()
    timeractor ! StartTimer
    println("ready!")

    unfiltered.netty.Http(setting.websocket.port).handler(unfiltered.netty.websockets.Planify({
      case _ => {
        case Open(s) =>
          val socketactor1 = actorOf(SocketActor(s)).start()
          socketactor1.id = socketActorName(s)
        case Message(_, _) =>
        case Close(s) =>
          registry.actorsFor(socketActorName(s)) map { actor =>
            actor.stop
          }
        case Error(s, e) =>
          e.printStackTrace
      }
    })
    .onPass(_.sendUpstream(_)))
    .handler(unfiltered.netty.cycle.Planify{
      case _ => ResponseString("not a websocket")
    })
    .run { s =>
    }
  }
}
