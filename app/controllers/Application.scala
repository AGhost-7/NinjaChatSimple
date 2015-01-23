package controllers

import scala.concurrent.Future

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._

import reactivemongo.api._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models._

object Application extends Controller with MongoController {
	
	import models.Implicits._
	
	val (out, channel) = Concurrent.broadcast[JsValue]
	
  def index = Action { 
  	Ok(views.html.index())
  }
	
	def socket(tokens: List[String]) = WebSocket.async[JsValue] { implicit request =>
		
		// Need to escape the text since this isn't going through the default 
		// template escaping. User names are already sanitized. Instead of going 
		// through the string several times, traverse it once.
		// Reference:
		// https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet
		def escape(msg: String) = {
			val r = new StringBuilder
			val i = msg.iterator
			while(i.hasNext){
				i.next match {
					case '&' => r.append("&amp;")
					case '<' => r.append("&lt;")
					case '>' => r.append("&gt;")
					case '"' => r.append("&quot;")
					case ''' => r.append("&#x27;")
					case '/' => r.append("&#x2F;")
					case c => r.append(c)
				}
			}
			r.toString
		}
		
		val in = Iteratee.foreach[JsValue] { msg =>
			msg.validate[UserMessage] match {
				case JsSuccess(message, _) => 
					// Take the user name from the tokens instead of relying on the client
					// to be "honest" and give its real name.
					User.fromTokens(message.userTokens).onSuccess { 
						case Some(user) =>
							val serverBroadcast = 
								ServerBroadcast(user.name, escape(message.content))
							channel push(Json.toJson(serverBroadcast))
						case None =>
							val serverBroadcast = 
								ServerBroadcast("*Anonymous*", escape(message.content))
							channel push(Json.toJson(serverBroadcast))
					}
					
				case e: JsError => 
					println("Syntax error...")
			}
		}
		
		
		User.fromTokens(tokens).map { userOpt =>
			userOpt.fold {
				val login = Json.obj("notification" ->
						"A new anonymous user has just joined.")
				channel push(login)
			} { user =>
				val login = Json.obj("notification" ->
					s"User ${user.name} has just joined the conversation.")
				channel push(login)
			}
			(in, out)
		}
		
		
	}
	
	def jsRoutes = Action { implicit request =>
		import routes.javascript._
		Ok(Routes.javascriptRouter("routes")(
			routes.javascript.Users.register,
			routes.javascript.Users.login,
			routes.javascript.Users.logout,
			routes.javascript.Users.name
		))
	}
	
}


