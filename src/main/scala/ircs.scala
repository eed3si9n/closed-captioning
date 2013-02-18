// https://github.com/scala-irc-bot/scala-irc-bot/blob/master/src/main/scala/net/mtgto/irc/Client.scala
// https://github.com/scala-irc-bot/scala-irc-bot/blob/master/src/main/scala/net/mtgto/irc/event/Event.scala

package com.eed3si9n.cc.irc

import org.jibble.pircbot.{PircBot, User => PircUser}
import java.util.Date

trait IrcClient { self =>
  def encoding: String
  def nickName: String
  def userName: String
  def hostName: String
  def port: Int

  def isConnected: Boolean = underlying.isConnected
  def connect() {
    underlying.connect(hostName, port)
  }
  def joinChannel(channel: String) {
    if (!isConnected) {
      connect()
    }
    underlying.joinChannel(channel)
  }

  def disconnect() {
    if (underlying.isConnected) {
      underlying.disconnect
    }
  }

  def onMessage(message: Message) {}
  def onPrivateMessage(message: PrivateMessage) {}
  def onNotice(notice: Notice) {}
  def onInvite(invite: Invite) {}
  def onJoin(join: Join) {}
  def onKick(kick: Kick) {}
  def onMode(mode: Mode) {}
  def onTopic(topic: Topic) {}
  def onNickChange(nickChange: NickChange) {}
  def onOp(op: Op) {}
  def onPart(part: Part) {}
  def onQuit(quit: Quit) {}
  def onUserList(userList: UserList) {}

  private[this] lazy val underlying = new RawClient()

  private class RawClient extends PircBot {
    setEncoding(encoding)
    setName(nickName)
    setLogin(userName)
    
    override protected def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) = {
      self.onMessage(Message(channel, sender, login, hostname, message, new Date))
    }

    override protected def onPrivateMessage(sender: String, login: String, hostname: String, message: String) = {
      self.onPrivateMessage(PrivateMessage(sender, login, hostname, message, new Date))
    }

    override protected def onNotice(sourceNick: String, sourceLogin: String, sourceHostname: String, target: String, notice: String) = {
      self.onNotice(Notice(target, sourceNick, sourceLogin, sourceHostname, notice, new Date))
    }

    override protected def onInvite(targetNick: String, sourceNick: String, sourceLogin: String, sourceHostname: String, channel: String) = {
      self.onInvite(Invite(channel, targetNick, sourceNick, sourceLogin, sourceHostname, new Date))
    }

    override protected def onJoin(channel: String, sender: String, login: String, hostname: String) = {
      self.onJoin(Join(channel, sender, login, hostname, new Date))
    }

    override protected def onKick(channel: String, kickerNick: String, kickerLogin: String, kickerHostname: String, recipientNick: String, reason: String) = {
      self.onKick(Kick(channel, recipientNick, kickerNick, kickerLogin, kickerHostname, reason, new Date))
    }

    override protected def onMode(channel: String, sourceNick: String, sourceLogin: String, sourceHostname: String, mode: String) = {
      self.onMode(Mode(channel, sourceNick, sourceLogin, sourceHostname, mode, new Date))
    }

    override protected def onTopic(channel: String, topic: String, setBy: String, date: Long, changed: Boolean) = {
      self.onTopic(Topic(channel, setBy, topic, new Date(date)))
    }

    override protected def onNickChange(oldNick: String, login: String, hostname: String, newNick: String) = {
      self.onNickChange(NickChange(oldNick, newNick, login, hostname, new Date))
    }

    override protected def onOp(channel: String, sourceNick: String, sourceLogin: String, sourceHostname: String, recipient: String) = {
      self.onOp(Op(channel, recipient, sourceNick, sourceLogin, sourceHostname, new Date))
    }

    override protected def onPart(channel: String, sender: String, login: String, hostname: String) = {
      self.onPart(Part(channel, sender, login, hostname, new Date))
    }

    override protected def onQuit(sourceNick: String, sourceLogin: String, sourceHostname: String, reason: String) = {
      self.onQuit(Quit(sourceNick, sourceLogin, sourceHostname, reason, new Date))
    }

    override protected def onUserList(channel: String, users: Array[PircUser]) = {
      self.onUserList(UserList(channel, users.map(_.getNick).toSeq, new Date))
    }

  }
}

sealed trait Event {
  val date = new Date
}

case class UserList(
  channel: String,
  nicknames: Seq[String],
  override val date: Date
) extends Event

/**
 * a message someone sends to a channel.
 */
case class Message(
  channel: String,
  nickName: String,
  userName: String,
  hostName: String,
  text: String,
  override val date: Date
) extends Event

/**
 * a private message someone sends to the irc bot.
 */
case class PrivateMessage(
  nickName: String,
  userName: String,
  hostName: String,
  text: String,
  override val date: Date
) extends Event

/**
 * a notice someone sends to a channel or user.
 */
case class Notice(
  target: String,
  sourceNickname: String,
  sourceUsername: String,
  sourceHostname: String,
  text: String,
  override val date: Date
) extends Event

/**
 * someone (source) invites the user (target) to a channel.
 */
case class Invite(
  channel: String,
  targetNickname: String,
  sourceNickname: String,
  sourceUsername: String,
  sourceHostname: String,
  override val date: Date
) extends Event

/**
 * someone joins to a channel.
 */
case class Join(
  channel: String,
  nickName: String,
  userName: String,
  hostName: String,
  override val date: Date
) extends Event

/**
 * someone (source) kicks the user (target) on a channel.
 */
case class Kick(
  channel: String,
  targetNickname: String,
  sourceNickname: String,
  sourceUsername: String,
  sourceHostname: String,
  reason: String,
  override val date: Date
) extends Event

/**
 * someone changes the mode of a channel.
 */
case class Mode(
  channel: String,
  nickName: String,
  userName: String,
  hostName: String,
  mode: String,
  override val date: Date
) extends Event

/**
 * a channel topic set by someone.
 */
case class Topic(
  channel: String,
  nickName: String,
  topic: String,
  override val date: Date
) extends Event

/**
 * someone changes his nickname.
 */
case class NickChange(
  oldNickname: String,
  newNickname: String,
  userName: String,
  hostName: String,
  override val date: Date
) extends Event

/**
 * someone (sourec) adds operator status to the user (target).
 */
case class Op(
  channel: String,
  targetNickname: String,
  sourceNickname: String,
  sourceUsername: String,
  sourceHostname: String,
  override val date: Date
) extends Event

/**
 * someone leaves a channel.
 */
case class Part(
  channel: String,
  nickName: String,
  userName: String,
  hostName: String,
  override val date: Date
) extends Event

/**
 * someone quit the connection to the server.
 */
case class Quit(
  nickName: String,
  userName: String,
  hostName: String,
  reason: String,
  override val date: Date
) extends Event

