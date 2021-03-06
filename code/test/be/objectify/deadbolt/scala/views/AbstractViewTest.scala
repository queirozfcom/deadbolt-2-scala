package be.objectify.deadbolt.scala.views

import be.objectify.deadbolt.core.models.{Permission, Role, Subject}
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.testhelpers.{FakeCache, User}
import be.objectify.deadbolt.scala.{DeadboltHandler, DeadboltModule, DynamicResourceHandler, HandlerKey}
import play.api.cache.CacheApi
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result, Results}
import play.api.test.PlaySpecification
import play.api.{Application, Mode}
import play.libs.Scala

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
class AbstractViewTest extends PlaySpecification {

  def testApp(handler: DeadboltHandler): Application = new GuiceApplicationBuilder()
                                                       .bindings(new DeadboltModule())
                                                       .bindings(bind[HandlerCache].toInstance(LightweightHandlerCache(handler)))
                                                       .overrides(bind[CacheApi].to[FakeCache])
                                                       .in(Mode.Test)
                                                       .build()

  def user(name: String = "foo",
           roles: List[_ <: Role] = List.empty,
           permissions: List[_ <: Permission] = List.empty): User =  User("foo", Scala.asJava(roles), Scala.asJava(permissions))

  def handler(beforeAuthCheck: Option[Result] = None,
              subject: Option[User] = None,
              onAuthFailure: Result = Results.Forbidden,
              drh: Option[DynamicResourceHandler] = None): DeadboltHandler = new LightweightHandler(Future(beforeAuthCheck),
                                                                                                    Future(subject),
                                                                                                    Future(onAuthFailure),
                                                                                                    Future(drh))
}

case class drh(allowed: Boolean, check: Boolean) extends DynamicResourceHandler {
  override def isAllowed[A](name: String, meta: String, deadboltHandler: DeadboltHandler, request: Request[A]): Future[Boolean] = Future(allowed)
  override def checkPermission[A](permissionValue: String, deadboltHandler: DeadboltHandler, request: Request[A]): Future[Boolean] = Future(check)
}

case class BasicHandlerKey(name: String) extends HandlerKey

case class LightweightHandler(bac: Future[Option[Result]],
                              subj: Future[Option[Subject]],
                              authFail: Future[Result],
                              drh: Future[Option[DynamicResourceHandler]]) extends DeadboltHandler {

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = bac
  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = drh
  override def getSubject[A](request: Request[A]): Future[Option[Subject]] = subj
  override def onAuthFailure[A](request: Request[A]): Future[Result] = authFail
}

case class LightweightHandlerCache(handler: DeadboltHandler) extends HandlerCache {
  override def apply() = handler
  override def apply(key: HandlerKey) = handler
}
