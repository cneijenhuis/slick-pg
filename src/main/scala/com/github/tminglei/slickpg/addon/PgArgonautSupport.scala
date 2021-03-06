package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

trait PgArgonautSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import argonaut._, Argonaut._

  def pgjson: String

  /// alias
  trait JsonImplicits extends ArgonautJsonImplicits

  trait ArgonautJsonImplicits {
    implicit val argonautJsonTypeMapper =
      new GenericJdbcType[Json](
        pgjson,
        (s) => s.parse.toOption.getOrElse(jNull),
        (v) => v.nospaces,
        hasLiteralForm = false
      )

    implicit def argonautJsonColumnExtensionMethods(c: Rep[Json])(
      implicit tm: JdbcType[Json], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[Json, Json](c)
      }
    implicit def argonautJsonOptionColumnExtensionMethods(c: Rep[Option[Json]])(
      implicit tm: JdbcType[Json], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[Json, Option[Json]](c)
      }
  }

  trait ArgonautJsonPlainImplicits {

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(jNull)
      def nextJsonOption() = r.nextStringOption().flatMap(_.parse.toOption)
    }

    implicit object SetJson extends SetParameter[Json] {
      def apply(v: Json, pp: PositionedParameters) = setJson(Option(v), pp)
    }
    implicit object SetJsonOption extends SetParameter[Option[Json]] {
      def apply(v: Option[Json], pp: PositionedParameters) = setJson(v, pp)
    }

    ///
    private def setJson(v: Option[Json], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject(pgjson, v.nospaces), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
  }
}
