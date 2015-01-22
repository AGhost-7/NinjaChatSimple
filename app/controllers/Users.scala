package controllers
import scala.concurrent.Future

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext


import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

import reactivemongo.api._

import org.mindrot.jbcrypt.BCrypt

import models._



object Users extends Controller with MongoController {
	
	import models.Implicits._
	
	def name(tokens: List[String]) = Action.async { implicit request =>
		User.fromTokens(tokens).map { optUser =>
			optUser.fold {
				BadRequest(Json.obj("message" -> "User name not found."))
			} { user =>
				Ok(Json.toJson(user.name))
			}
		}
	}
	
	def register(name: String, password: String) = Action.async { implicit request =>
		val error = User.findError(name, password)
		if(error.isDefined){
			// A 400? Hmm.
			val result = BadRequest(Json.obj("message" -> error.get))
			Future.successful(result)
		}	else {
			// First I need to ensure that the user name isn't already in use.
			// I think that in the long run I'll want to ensure unique user name
			// through database rules.
			User.collection.find(Json.obj("name" -> name)).one[User].flatMap { optUser =>
				optUser.fold {
					for {
						user <- User.insert(name, password)
						token <- Token.generate(user)
					} yield Ok(Json.toJson(token))
					
				} { user =>
					// Send a 409 error... I think its appropriate for a duplicate error.
					val result = Conflict(
							Json.obj(
									"message" -> 
										"A user with your given account name already exists."))
					
					Future.successful(result)
				}
			}
		}
	}
	
	def login(name: String, password: String) = Action.async { implicit request =>
		User.collection.find(Json.obj("name" -> name)).one[User].flatMap { optUser =>
			optUser.fold {
				val message = "User name does not exist in database."
				Future.successful(Unauthorized(Json.obj("message" -> message)))
			} { user =>
				if(BCrypt.checkpw(password, user.password)) {
					// Name exists and passord is correct, just need to give this guy his
					// token now.
					Token.generate(user).map { token =>
						Ok(Json.toJson(token))
					}
				} else {
					val result = Unauthorized(Json.obj("message" -> "Password is invalid."))
					Future.successful(result)
				}
			}
		}
	}
	
	def logout(tokens: List[String]) = Action.async { implicit request =>
		User.fromTokens(tokens).flatMap { optUser =>
			optUser.fold {
				// So we don't seem to have you as a user in the database, I'll clear
				// your cookies but can't do more than that...
				val message = "Are you sure you're logged in properly?"
				
				Future.successful(Unauthorized(Json.obj("message" -> message)))
			} { user =>
				// So, got some work to do. For now this will remove all tokens tied to
				// user. Not sure how to do bulk removal, although there seems to be
				// a bulk insert.
				Token.collection.remove(Json.obj("userId" -> user._id)).map { lastError =>
					Ok(Json.obj("message" -> "Logged out without any issues!"))
				}
			}
		}
	}
}











