package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{PositionedResult, SetParameter, PositionedParameters, JdbcType}

trait PgJson4sSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import org.json4s._

  type DOCType
  def pgjson: String

  val jsonMethods: JsonMethods[DOCType]

  /// alias
  trait JsonImplicits extends Json4sJsonImplicits

  trait Json4sJsonImplicits {
    implicit val json4sJsonTypeMapper =
      new GenericJdbcType[JValue](
        pgjson,
        (s) => jsonMethods.parse(s),
        (v) => jsonMethods.compact(jsonMethods.render(v)),
        hasLiteralForm = false
      )

    implicit def json4sJsonColumnExtensionMethods(c: Rep[JValue])(
      implicit tm: JdbcType[JValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JValue, JValue](c)
      }
    implicit def json4sJsonOptionColumnExtensionMethods(c: Rep[Option[JValue]])(
      implicit tm: JdbcType[JValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JValue, Option[JValue]](c)
      }
  }

  trait Json4sJsonPlainImplicits {

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JNull)
      def nextJsonOption() = r.nextStringOption().map(jsonMethods.parse(_))
    }

    implicit object SetJson extends SetParameter[JValue] {
      def apply(v: JValue, pp: PositionedParameters) = setJson(Option(v), pp)
    }
    implicit object SetJsonOption extends SetParameter[Option[JValue]] {
      def apply(v: Option[JValue], pp: PositionedParameters) = setJson(v, pp)
    }

    ///
    private def setJson(v: Option[JValue], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject(pgjson, jsonMethods.compact(jsonMethods.render(v))), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
  }
}
