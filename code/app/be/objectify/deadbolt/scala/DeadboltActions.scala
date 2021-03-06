package be.objectify.deadbolt.scala

import javax.inject.{Inject, Singleton}

import be.objectify.deadbolt.core.PatternType
import be.objectify.deadbolt.core.models.Subject
import be.objectify.deadbolt.scala.cache.HandlerCache
import play.api.mvc.{Action, BodyParsers, Result, Results}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 * Controller-level authorisations for Scala controllers.
 *
 * @author Steve Chaloner
 */
@Singleton
class DeadboltActions @Inject()(analyzer: ScalaAnalyzer, handlers: HandlerCache) extends Results with BodyParsers {

  /**
   * Restrict access to an action to users that have all the specified roles.
   *
   * @param roleNames
   * @param handler the handler to use for constraint testing
   * @param action the wrapped action
   * @tparam A
   * @return
   */
  def Restrict[A](roleNames: Array[String],
                  handler: DeadboltHandler)(action: Action[A]): Action[A] = Restrict[A](List(roleNames),
                                                                                                handler)(action)

  /**
   * Restrict access to an action to users that have all the specified roles within a given group.  Each group, which is
   * an array of strings, is checked in turn.
   *
   * @param roleGroups
   * @param handler the handler to use for constraint testing
   * @param action the wrapped action
   * @tparam A
   * @return
   */
  def Restrict[A](roleGroups: List[Array[String]],
                  handler: DeadboltHandler = handlers())(action: Action[A]): Action[A] = {
    Action.async(action.parser) { implicit request =>

      def check(subject: Option[Subject], current: Array[String], remaining: List[Array[String]]): Future[Result] = {
        if (analyzer.checkRole(subject, current)) action(request)
        else if (remaining.isEmpty) handler.onAuthFailure(request)
        else check(subject, remaining.head, remaining.tail)
      }

      handler.beforeAuthCheck(request).flatMap((beforeAuthOption: Option[Result]) => {
        beforeAuthOption match {
          case Some(result) => Future(result)
          case None =>
            if (roleGroups.isEmpty) handler.onAuthFailure(request)
            else {
              handler.getSubject(request).flatMap((subjectOption: Option[Subject]) =>
                                                            subjectOption match {
                                                              case Some(subject) => check(subjectOption, roleGroups.head, roleGroups.tail)
                                                              case _ => handler.onAuthFailure(request)
                                                            })
            }
        }
      })
                                }
  }

  /**
   *
   * @param name the name of the dynamic constraint
   * @param meta additional information
   * @param handler the handler to use for constraint testing
   * @param action the wrapped action
   * @tparam A
   * @return
   */
  def Dynamic[A](name: String,
                 meta: String = "",
                 handler: DeadboltHandler = handlers())(action: Action[A]): Action[A] = {
    Action.async(action.parser) { implicit request =>

      handler.beforeAuthCheck(request).flatMap((beforeAuthOption: Option[Result]) => {
        beforeAuthOption match {
          case Some(result) => Future(result)
          case None =>
            handler.getDynamicResourceHandler(request).flatMap((drhOption: Option[DynamicResourceHandler]) => {
              drhOption match {
                case Some(dynamicHandler) =>
                  dynamicHandler.isAllowed(name, meta, handler, request).flatMap((allowed: Boolean) => allowed match {
                    case true => action(request)
                    case false => handler.onAuthFailure(request)
                  })
                case None =>
                  throw new RuntimeException("A dynamic resource is specified but no dynamic resource handler is provided")
              }
            })
        }
      })
                                }
  }

  /**
   *
   * @param value the value of the pattern, e.g. a regex
   * @param patternType the type of the pattern
   * @param handler the handler to use for constraint testing
   * @param action the wrapped action
   * @tparam A
   * @return
   */
  def Pattern[A](value: String,
                 patternType: PatternType,
                 handler: DeadboltHandler = handlers())(action: Action[A]): Action[A] = {

    Action.async(action.parser) {
                                  implicit request =>
                                    handler.beforeAuthCheck(request).flatMap((beforeAuthOption: Option[Result]) => {
                                      beforeAuthOption match {
                                        case Some(result) => Future(result)
                                        case None =>
                                          handler.getSubject(request).flatMap((subjectOption: Option[Subject]) => subjectOption match {
                                            case None => handler.onAuthFailure(request)
                                            case Some(subject) => patternType match {
                                              case PatternType.EQUALITY =>
                                                if (analyzer.checkPatternEquality(subjectOption, Option(value))) action(request)
                                                else handler.onAuthFailure(request)
                                              case PatternType.REGEX =>
                                                if (analyzer.checkRegexPattern(subjectOption, value)) action(request)
                                                else handler.onAuthFailure(request)
                                              case PatternType.CUSTOM =>
                                                handler.getDynamicResourceHandler(request).flatMap((drhOption: Option[DynamicResourceHandler]) => {
                                                  drhOption match {
                                                    case Some(dynamicHandler) =>
                                                      dynamicHandler.checkPermission(value, handler, request).flatMap((allowed: Boolean) => {
                                                        allowed match {
                                                          case true => action(request)
                                                          case false => handler.onAuthFailure(request)
                                                        }
                                                      })
                                                    case None =>
                                                      throw new RuntimeException("A custom pattern is specified but no dynamic resource handler is provided")
                                                  }
                                                })
                                            }
                                          })
                                      }
                                    })
                                }
  }

  /**
   * Allows access to the action if there is a subject present.
   *
   * @param handler the handler to use for constraint testing
   * @param action the wrapped action
   * @tparam A
   * @return
   */
  def SubjectPresent[A](handler: DeadboltHandler = handlers())(action: Action[A]): Action[A] = {
    Action.async(action.parser) { implicit request =>
      handler.beforeAuthCheck(request).flatMap((beforeAuthOption: Option[Result]) =>
                                                         beforeAuthOption match {
                                                           case Some(result) => Future(result)
                                                           case None =>
                                                             handler.getSubject(request).flatMap((subjectOption: Option[Subject]) => subjectOption match {
                                                               case Some(subject) => action(request)
                                                               case None => handler.onAuthFailure(request)
                                                             })
                                                         })
                                }
  }

  /**
   * Denies access to the action if there is a subject present.
   *
   * @param handler the handler to use for constraint testing
   * @param action the wrapped action
   * @tparam A
   * @return
   */
  def SubjectNotPresent[A](handler: DeadboltHandler = handlers())(action: Action[A]): Action[A] = {
    Action.async(action.parser) { implicit request =>
      handler.beforeAuthCheck(request).flatMap((beforeAuthOption: Option[Result]) =>
                                                         beforeAuthOption match {
                                                           case Some(result) => Future(result)
                                                           case None =>
                                                             handler.getSubject(request).flatMap((subjectOption: Option[Subject]) => subjectOption match {
                                                               case Some(subject) => handler.onAuthFailure(request)
                                                               case None => action(request)
                                                             })
                                                         })
                                }
  }
}
