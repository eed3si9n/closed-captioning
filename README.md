closed-captioning
=================

closed-captioning is a tweet + IRC aggregator, intended for conference talks. this is inspired by [ruby-no-kai/kaigi_subscreen](https://github.com/ruby-no-kai/kaigi_subscreen).

hashtag is a fun way of getting audience feedback, but it's not meant to be reliable or responsive, partially due to twitter's hourly API limits.　IRC on the other hand, while not as accessible as twitter, is more reliable and responsive.

![screenshot](https://raw.github.com/eed3si9n/closed-captioning/master/screenshot.png)

how to setup closed-captioning
------------------------------

### sbt-twt

run sbt:

    > twt log
    accept the authorization request in your browser, for the fun to begin.
    (brower should pop up asking if you want to let sbt-twt access you account)
    then run `twt pin <pin>` to complete the process.
    > twt pin 8686743

this creates a configuration to grab tweets as you.

### application.config

under `conf` directory create a file named `application.conf`:

```
closed-captioning {
  websocket {
    port = 5679
  }

  irc {
    nickname = "your_bot_name"
    username = "your_bot_name"
    hostname = "irc.freenode.net"
    port = 6667 # or 7000
    use_ssl = false # or true
    encoding = "UTF-8"
    channel = "##your_channel"
  }

  twitter {
    search-query = "#scala"
    count = 5
    interval = 60s
  }
}
```

the nickname and the username cannot be your own nickname.

### irc

using your favorite IRC client, join the specified channel as yourself.

### starting websocket server

run `re-start` from sbt shell to start the websocket server in the background:

    > project closed-captioning
    > re-start
    [info] Application not yet started
    [info] Starting application in the background ...
    app: Starting com.eed3si9n.cc.App.main()
    app: [2013-02-18 06:44:00] Contacting myJRebel server ..
    ....
    app: ready!

this might take a little time. once the server is up, you should see your bot joining the IRC channel.

### JavaScript client

open http://eed3si9n.com/closed-captioning/ using Chrome.
if it says "connection open," then we are in business.
start typing something into the IRC channel.
