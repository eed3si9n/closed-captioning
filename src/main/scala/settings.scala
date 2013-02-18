package com.eed3si9n.cc

import com.typesafe.config.{Config, ConfigFactory}

class CCSetting(config: Config) {
  config.checkValid(ConfigFactory.defaultReference(), "closed-captioning")

  private[this] val cc = config.getConfig("closed-captioning")
  
  object websocket {
    private[this] val c = cc.getConfig("websocket")
    val port = c.getInt("port")
  }

  object irc {
    private[this] val c = cc.getConfig("irc")  
    val nickname = c.getString("nickname")
    val username = c.getString("username")
    val hostname = c.getString("hostname")
    val port = c.getInt("port")
    val encoding = c.getString("encoding")
    val channel = c.getString("channel")
  }

  object twitter {
    private[this] val c = cc.getConfig("twitter")
    val searchQuery = c.getString("search-query")
    val count = c.getInt("count")
    val interval = c.getMilliseconds("interval")
  }
}
