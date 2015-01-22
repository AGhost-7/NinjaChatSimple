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
	
	def socket = WebSocket.using[JsValue] { implicit request =>
		println("Connected!")
		val in = Iteratee.foreach[JsValue] { msg =>
			
			msg.validate[UserMessage] match {
				case JsSuccess(message, _) => 
					// Take the user name from the tokens instead of relying on the client
					// to be "honest" and give its real name.
					User.fromTokens(message.userTokens).onSuccess { 
						case Some(user) =>
							val serverBroadcast = ServerBroadcast(user.name, message.content)
							channel push(Json.toJson(serverBroadcast))
						case None =>
							val serverBroadcast = ServerBroadcast("*Anonymous*", message.content)
							channel push(Json.toJson(serverBroadcast))
					}
					
				case e: JsError => 
					println("Syntax error...")
			}
		}
		
		(in, out)
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


