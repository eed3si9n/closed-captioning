package com.eed3si9n.cc

object App {
  import unfiltered.netty.websockets._
  import unfiltered.util._
  import unfiltered.response.ResponseString
  import akka.actor._
  import akka.actor.Actor._
  import akka.util.duration._
  import com.typesafe.config.ConfigFactory

  implicit val timeout = Timeout(100 millisecond)

  def socketActorName(s: WebSocket): String = "socketactor" + s.channel.getId.toString
    
  def main(args: Array[String]) {
    val setting = new CCSetting(ConfigFactory.load())

    val tweetactor = actorOf(TweetActor()).start()
    val timeractor = actorOf(TimerActor(
      setting.twitter.searchQuery, setting.twitter.count,
      setting.twitter.interval, tweetactor)).start()
    val ircactor = actorOf(IrcActor(
      setting.irc.encoding, setting.irc.nickname, setting.irc.username,
      setting.irc.hostname, setting.irc.port, setting.irc.channel
    )).start()
    timeractor ! StartTimer

    unfiltered.netty.Http(setting.websocket.port).handler(unfiltered.netty.websockets.Planify({
      case _ => {
        case Open(s) =>
          val socketactor1 = actorOf(SocketActor(s)).start()
          socketactor1.id = socketActorName(s)
          tweetactor ! AddSocket(socketactor1)
          ircactor ! AddSocket(socketactor1)
        case Message(_, _) =>
        case Close(s) =>
          registry.actorsFor(socketActorName(s)) map { actor =>
            tweetactor ! RemoveSocket(actor)
            ircactor ! RemoveSocket(actor)
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
