package sttp.tapir.codegen.openapi.models

import cats.implicits.toTraverseOps
import cats.syntax.either._

import OpenapiSchemaType.OpenapiSchemaRef
// https://swagger.io/specification/
object OpenapiModels {
  sealed trait SpecificationExtensionValue {
    def tpe: String
    def render: String
    def value: Any
  }
  case object SpecificationExtensionValueNull extends SpecificationExtensionValue {
    val tpe = "Null"
    val render = "null"
    val value = null
  }
  case class SpecificationExtensionValueBoolean(value: Boolean) extends SpecificationExtensionValue {
    val render = value.toString
    val tpe = "Boolean"
  }
  case class SpecificationExtensionValueLong(value: Long) extends SpecificationExtensionValue {
    val render = s"${value}L"
    val tpe = "Long"
  }
  case class SpecificationExtensionValueDouble(value: Double) extends SpecificationExtensionValue {
    val render = s"${value}d"
    val tpe = "Double"
  }
  case class SpecificationExtensionValueString(value: String) extends SpecificationExtensionValue {
    val render = '"' +: value :+ '"'
    val tpe = "String"
  }
  case class SpecificationExtensionValueList(values: Seq[SpecificationExtensionValue]) extends SpecificationExtensionValue {
    val render = s"Vector(${values.map(_.render).mkString(", ")})"
    def tpe = values.map(_.tpe).distinct match { case single +: Nil => s"Seq[$single]"; case _ => "Seq[Any]" }
    def value = values.map(_.value)
  }
  case class SpecificationExtensionValueMap(kvs: Map[String, SpecificationExtensionValue]) extends SpecificationExtensionValue {
    val render = s"Map(${kvs.map { case (k, v) => s""""$k" -> ${v.render}""" }.mkString(", ")})"
    def tpe = kvs.values.map(_.tpe).toSeq.distinct match { case single +: Nil => s"Map[String, $single]"; case _ => "Map[String, Any]" }
    def value = kvs.map { case (k, v) => k -> v.value }
  }

  sealed trait Resolvable[T] {
    def resolve(input: Map[String, T]): T
    def toResolved(input: Map[String, T]): Resolved[T] = Resolved(resolve(input))
  }
  case class Resolved[T](t: T) extends Resolvable[T] {
    override def resolve(input: Map[String, T]): T = t
  }
  case class Ref[T](name: String) extends Resolvable[T] {
    override def resolve(input: Map[String, T]): T = input.getOrElse(name, throw new IllegalArgumentException(s"Cannot resolve $name"))
  }

  case class OpenapiDocument(
      openapi: String,
      // not used so not parsed; servers, contact, license, termsOfService
      info: OpenapiInfo,
      paths: Seq[OpenapiPath],
      components: Option[OpenapiComponent]
  )

  case class OpenapiInfo(
      // not used so not parsed; description
      title: String,
      version: String
  )

  case class OpenapiPath(
      url: String,
      methods: Seq[OpenapiPathMethod],
      parameters: Seq[Resolvable[OpenapiParameter]] = Nil,
      specificationExtensions: Map[String, SpecificationExtensionValue] = Map.empty
  )

  case class OpenapiPathMethod(
      methodType: String,
      parameters: Seq[Resolvable[OpenapiParameter]],
      responses: Seq[OpenapiResponse],
      requestBody: Option[OpenapiRequestBody],
      security: Seq[Seq[String]] = Nil,
      summary: Option[String] = None,
      tags: Option[Seq[String]] = None,
      operationId: Option[String] = None,
      specificationExtensions: Map[String, SpecificationExtensionValue] = Map.empty
  ) {
    def resolvedParameters: Seq[OpenapiParameter] = parameters.collect { case Resolved(t) => t }
    def withResolvedParentParameters(
        pMap: Map[String, OpenapiParameter],
        pathParameters: Seq[Resolvable[OpenapiParameter]]
    ): OpenapiPathMethod = {
      val resolved = parameters.map(_.toResolved(pMap))
      val duplicates = resolved.groupBy(_.t.name).filter(_._2.size > 1).keys
      if (duplicates.nonEmpty) throw new IllegalArgumentException(s"Duplicate parameters ${duplicates.mkString(", ")}")
      val filteredParents: Seq[Resolved[OpenapiParameter]] =
        pathParameters.map(_.toResolved(pMap)).filterNot(p => resolved.exists(p.t.name == _.t.name))
      val parentDuplicates = filteredParents.groupBy(_.t.name).filter(_._2.size > 1).keys
      if (parentDuplicates.nonEmpty) throw new IllegalArgumentException(s"Duplicate parameters ${parentDuplicates.mkString(", ")}")
      this.copy(parameters = filteredParents ++ resolved)
    }
  }

  case class OpenapiParameter(
      name: String,
      in: String,
      required: Option[Boolean],
      description: Option[String],
      schema: OpenapiSchemaType
  )

  case class OpenapiResponse(
      code: String,
      description: String,
      content: Seq[OpenapiResponseContent]
  )

  case class OpenapiRequestBody(
      required: Boolean,
      description: Option[String],
      content: Seq[OpenapiRequestBodyContent]
  )

  case class OpenapiResponseContent(
      contentType: String,
      schema: OpenapiSchemaType
  )

  case class OpenapiRequestBodyContent(
      contentType: String,
      schema: OpenapiSchemaType
  )

  // ///////////////////////////////////////////////////////
  // decoders
  // //////////////////////////////////////////////////////

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val OpenapiResponseContentDecoder: Decoder[Seq[OpenapiResponseContent]] = { (c: HCursor) =>
    case class Holder(d: OpenapiSchemaType)
    implicit val InnerDecoder: Decoder[Holder] = { (c: HCursor) =>
      for {
        schema <- c.downField("schema").as[OpenapiSchemaType]
      } yield {
        Holder(schema)
      }
    }
    for {
      responses <- c.as[Map[String, Holder]]
    } yield {
      responses.map { case (ct, s) => OpenapiResponseContent(ct, s.d) }.toSeq
    }
  }

  implicit val OpenapiResponseDecoder: Decoder[Seq[OpenapiResponse]] = { (c: HCursor) =>
    implicit val InnerDecoder: Decoder[(String, Option[Seq[OpenapiResponseContent]])] = { (c: HCursor) =>
      for {
        description <- c.downField("description").as[String]
        content <- c.downField("content").as[Option[Seq[OpenapiResponseContent]]]
      } yield {
        (description, content)
      }
    }
    for {
      schema <- c.as[Map[String, (String, Option[Seq[OpenapiResponseContent]])]]
    } yield {
      schema.map { case (code, (desc, content)) =>
        OpenapiResponse(code, desc, content.getOrElse(Nil))
      }.toSeq
    }
  }

  implicit val OpenapiRequestBodyContentDecoder: Decoder[Seq[OpenapiRequestBodyContent]] = { (c: HCursor) =>
    case class Holder(d: OpenapiSchemaType)
    implicit val InnerDecoder: Decoder[Holder] = { (c: HCursor) =>
      for {
        schema <- c.downField("schema").as[OpenapiSchemaType]
      } yield {
        Holder(schema)
      }
    }
    for {
      responses <- c.as[Map[String, Holder]]
    } yield {
      responses.map { case (ct, s) => OpenapiRequestBodyContent(ct, s.d) }.toSeq
    }
  }

  implicit val OpenapiRequestBodyDecoder: Decoder[OpenapiRequestBody] = { (c: HCursor) =>
    for {
      requiredOpt <- c.downField("required").as[Option[Boolean]]
      description <- c.downField("description").as[Option[String]]
      content <- c.downField("content").as[Seq[OpenapiRequestBodyContent]]
    } yield {
      OpenapiRequestBody(required = requiredOpt.getOrElse(false), description, content)
    }
  }

  implicit val OpenapiInfoDecoder: Decoder[OpenapiInfo] = deriveDecoder[OpenapiInfo]
  implicit val OpenapiParameterDecoder: Decoder[OpenapiParameter] = deriveDecoder[OpenapiParameter]
  implicit def ResolvableDecoder[T: Decoder]: Decoder[Resolvable[T]] = { (c: HCursor) =>
    c.as[T].map(Resolved(_)).orElse(c.as[OpenapiSchemaRef].map(r => Ref(r.name)))
  }

  def decodeSpecificationExtensionValue(json: Json): SpecificationExtensionValue =
    json.fold(
      SpecificationExtensionValueNull,
      SpecificationExtensionValueBoolean.apply,
      n => n.toLong.map(SpecificationExtensionValueLong.apply).getOrElse(SpecificationExtensionValueDouble(n.toDouble)),
      SpecificationExtensionValueString.apply,
      arr => SpecificationExtensionValueList(arr.map(decodeSpecificationExtensionValue)),
      obj => SpecificationExtensionValueMap(obj.toMap.map { case (k, v) => k -> decodeSpecificationExtensionValue(v) })
    )
  implicit val SpecificationExtensionValueDecoder: Decoder[SpecificationExtensionValue] = { (c: HCursor) =>
    Right(decodeSpecificationExtensionValue(c.value))
  }
  implicit val PartialOpenapiPathMethodDecoder: Decoder[OpenapiPathMethod] = { (c: HCursor) =>
    for {
      parameters <- c.getOrElse[Seq[Resolvable[OpenapiParameter]]]("parameters")(Nil)
      responses <- c.get[Seq[OpenapiResponse]]("responses")
      requestBody <- c.get[Option[OpenapiRequestBody]]("requestBody")
      security <- c.getOrElse[Seq[Map[String, Seq[String]]]]("security")(Nil)
      summary <- c.get[Option[String]]("summary")
      tags <- c.get[Option[Seq[String]]]("tags")
      operationId <- c.get[Option[String]]("operationId")
      specificationExtensionKeys = c.keys.toSeq.flatMap(_.filter(_.startsWith("x-")))
      specificationExtensions = specificationExtensionKeys
        .flatMap(key => c.downField(key).as[SpecificationExtensionValue].toOption.map(key.stripPrefix("x-") -> _))
        .toMap
    } yield {
      OpenapiPathMethod(
        "--partial--",
        parameters,
        responses,
        requestBody,
        security.map(_.keys.toSeq),
        summary,
        tags,
        operationId,
        specificationExtensions
      )
    }
  }

  implicit val PartialOpenapiPathDecoder: Decoder[OpenapiPath] = { (c: HCursor) =>
    for {
      parameters <- c
        .downField("parameters")
        .as[Option[Seq[Resolvable[OpenapiParameter]]]]
        .map(_.getOrElse(Nil))
      methods <- List("get", "put", "post", "delete", "options", "head", "patch", "connect", "trace")
        .traverse(method => c.downField(method).as[Option[OpenapiPathMethod]].map(_.map(_.copy(methodType = method))))
      specificationExtensionKeys = c.keys.toSeq.flatMap(_.filter(_.startsWith("x-")))
      specificationExtensions = specificationExtensionKeys
        .flatMap(key => c.downField(key).as[SpecificationExtensionValue].toOption.map(key.stripPrefix("x-") -> _))
        .toMap
    } yield OpenapiPath("--partial--", methods.flatten, parameters, specificationExtensions)
  }

  implicit val OpenapiPathsDecoder: Decoder[Seq[OpenapiPath]] = { (c: HCursor) =>
    for {
      paths <- c.as[Map[String, OpenapiPath]]
    } yield {
      paths.map { case (url, path) => path.copy(url = url) }.toSeq
    }
  }

  implicit val OpenapiDocumentDecoder: Decoder[OpenapiDocument] = { (c: HCursor) =>
    for {
      openapi <- c.downField("openapi").as[String]
      info <- c.downField("info").as[OpenapiInfo]
      paths <- c.downField("paths").as[Seq[OpenapiPath]]
      components <- c.downField("components").as[Option[OpenapiComponent]]
    } yield OpenapiDocument(openapi, info, paths, components)
  }

}
