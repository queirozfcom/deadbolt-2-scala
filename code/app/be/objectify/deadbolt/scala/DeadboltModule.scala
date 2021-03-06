package be.objectify.deadbolt.scala

import be.objectify.deadbolt.scala.cache.{DefaultPatternCache, PatternCache}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
class DeadboltModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[PatternCache].to[DefaultPatternCache],
    bind[ScalaAnalyzer].toSelf,
    bind[DeadboltActions].toSelf,
    bind[ViewSupport].toSelf,
    bind[ActionBuilders].toSelf,
    bind[TemplateFailureListenerProvider].toSelf
  )
}
