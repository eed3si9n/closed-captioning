package com.eed3si9n.cc

import java.io.File

case class Twt {
  import dispatch.classic._
  import json._
  import json.JsHttp._
  import oauth._
  import oauth.OAuth._
  import twitter._

  // import all the methods, including implicit conversions, defined on dispatch.Http
  import Http._

  val home = :/("twitter.com")
  // OAuth application key, top-secret
  val consumer = Consumer("Km1qVJOqYKDtFhlx6W6s3Q", "rgru7uaRGGm2fhrDrti4m8H5b8YliiohQyBcM17gBg")
  // one single-threaded http access point, please!
  val http = new Http with NoLogging
  val configFile = new File(System.getProperty("user.home"), ".twt.xml")
  
  val config = new TwtConfig(configFile)
  def auth_token: Option[Token] = Token(config.section("access").toMap)

  def grep(q: String, count: BigDecimal)(implicit token: Token): Seq[StatusResponse] = {
    for {
      item <- http(Search(consumer, token, q, ("show_user", "true"), ("rpp", count.toString)))
      id = Search.id(item)
      msg = Search.text(item)
      from_user = Search.from_user(item)
    } yield StatusResponse(id, msg, from_user)
  }

  def anonymousGrep(q: String, count: BigDecimal): Seq[StatusResponse] = {
    for {
      item <- http(Search(q, ("show_user", "true"), ("rpp", count.toString)))
      id = Search.id(item)
      msg = Search.text(item)
      from_user = Search.from_user(item)
    } yield StatusResponse(id, msg, from_user)
  }

  def formatTweet(id: BigDecimal, status: String, screenName: String): List[String] =
    "* %-22s %s".format("<" + screenName + ">", id.toString) ::
    "- " + Status.rebracket(status) ::
    "" :: Nil
}

case class StatusResponse(id: BigDecimal, status: String, screenName: String)

class TwtConfig(fileName: File) {
  import scala.collection.mutable
  import scala.xml.NodeSeq

  val sections = mutable.Map[String, mutable.Map[String, String]]()
  
  def section(k: String): mutable.Map[String, String] =
    sections getOrElseUpdate (k, mutable.Map[String, String]())
  
  def write() {
    val writer = new java.io.FileWriter(fileName)
    writer write (toNode.toString)
    writer.close
  }
  
  def toNode: NodeSeq = {
    def kvToNode(k: String, v: String): NodeSeq = <value name={k}>{v}</value>
    def sectionToNode(k: String): NodeSeq =
      <section name={k}>{ section(k).toSeq map { case (k, v) => kvToNode(k, v) } }</section>
    
    val xml: NodeSeq = <config>{ sections.toSeq map { case (k, v) => sectionToNode(k) } }</config>
    xml
  }
  
  def fromNode(node: NodeSeq) {
    for {
      s <- node \ "section"
      sn <- s \ "@name"
      v <- s \ "value"
      k <- v \ "@name"
    } section(sn.text)(k.text) = v.text
  }
  
  if (!fileName.exists()) write()
  
  fromNode(scala.xml.XML.loadFile(fileName))
}
