package dispatch.classic.twitter
import dispatch.classic._

import json._
import JsHttp._
import oauth._
import oauth.OAuth._
import dispatch.Request._

object Twitter {
  val host = :/("api.twitter.com").secure
  // val search = :/("api.twitter.com") / "1.1" / "search"
}

object Search extends Js {
  def apply(consumer: Consumer, token: Token, query: String, params: (String, String)*): SearchBuilder =
    new SearchBuilder(Some(consumer), Some(token), Map(params: _*), Map.empty[String, String]).q(query)
  def apply(query: String, params: (String, String)*): SearchBuilder =
    new SearchBuilder(None, None, Map(params: _*), Map.empty[String, String]).q(query)

  class SearchBuilder(cns: Option[Consumer], tkn: Option[Token], params: Map[String, String], headers: Map[String, String]) extends Builder[Handler[List[JsObject]]] {
    private def param(key: String)(value: Any): SearchBuilder = new SearchBuilder(cns, tkn, params + (key -> value.toString), headers)
    private def header(key: String)(value: String): SearchBuilder = new SearchBuilder(cns, tkn, params, headers + (key -> value))
    val q = param("q")_
    val lang = param("lang")_
    val rpp = param("rpp")_
    val page = param("page")_
    /** search rate limit conditions are higher for unique userAgents */
    val user_agent = header("User-Agent")_
    val since_id = param("since_id")_
    private def geocode0(unit: String)(lat: Double, lon: Double, radius: Double) =
      param("geocode")(List(lat, lon, radius).mkString(",") + unit)
    val geocode = geocode0("km")_
    val geocode_mi = geocode0("mi")_
    def product = 
      (cns, tkn) match {
        case (Some(consumer), Some(token)) =>  
          Twitter.host / "1.1" / "search" / "tweets.json" <<? params <@ (consumer, token) <:< headers ># ('statuses ! (list ! obj))
        case _ =>
          Twitter.host / "1.1" / "search" / "tweets.json" <<? params <:< headers ># ('statuses ! (list ! obj))
      }    
  }

  val to_user_id = 'to_user_id ? num
  val from_user_id = 'from_user_id ? num
  val source = 'source ? str
  val id = 'id ? num
  val text = 'text ? str
  val created_at = 'created_at ? str
  val iso_language_code = 'iso_language_code ? str
  val from_user = 'from_user ? str
  val profile_image_url = 'profile_image_url ? str

  def rebracket(status: String) = status replace ("&gt;", ">") replace ("&lt;", "<")
}

object Status extends Request(Twitter.host / "1.1" / "statuses") {
  private def public_timeline = this / "public_timeline.json" ># (list ! obj)

  def friends_timeline(consumer: Consumer, token: Token, params: (String, String)*) =
    new FriendsTimelineBuilder(consumer, token, Map(params: _*))

  class FriendsTimelineBuilder(consumer: Consumer, token: Token, params: Map[String, String])
      extends Builder[Handler[List[JsObject]]] {

    private def param(key: String)(value: Any) =
      new FriendsTimelineBuilder(consumer, token, params + (key -> value.toString))
    val since_id = param("since_id")_
    val max_id = param("max_id")_
    val count = param("count")_
    val page = param("page")_
    def product =  Status / "friends_timeline.json" <<? params <@ (consumer, token) ># (list ! obj)
  }

  def update(status: String, consumer: Consumer, token: Token) =
    this / "update.json" << Map("status" -> status) <@ (consumer, token)

  val text = 'text ? str
  val id = 'id ? num
  val user = 'user ? obj
  def rebracket(status: String) = status replace ("&gt;", ">") replace ("&lt;", "<")
}

case class Status(user_id: String) extends
    Request(Status / "user_timeline" / (user_id + ".json")) with Js {

  def timeline = this ># (list ! obj)
}

object User {
  val followers_count = 'followers_count ? num
  val screen_name = 'screen_name ? str
  val profile_image_url = 'profile_image_url ? str
}

case class User(user: String) extends
    Request(Twitter.host / "users" / "show" / (user + ".json")) with Js {

  val screen_name = 'screen_name ? str
  val profile_image_url = 'profile_image_url ? str
  def show = this ># obj
}

object Account extends
   Request(Twitter.host / "account") with Js {
   /** ip based rate limit status */
   def rate_limit_status = this / "rate_limit_status.json" ># obj
   /** authenticated user rate limit status */
   def rate_limit_status_as(consumer: Consumer, token: Token) =
     this / "rate_limit_status.json" <@(consumer, token) ># obj
}

object RateLimitStatus extends Js {
  val remaining_hits = 'remaining_hits ? num
  val reset_time_in_seconds = 'reset_time_in_seconds ? num
  val hourly_limit = 'hourly_limit ? num
  val reset_time = 'reset_time ? str
}

