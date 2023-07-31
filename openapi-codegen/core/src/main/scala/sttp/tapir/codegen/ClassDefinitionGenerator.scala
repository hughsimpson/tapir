package sttp.tapir.codegen

import sttp.tapir.codegen.BasicGenerator.{indent, mapSchemaSimpleTypeToType}
import sttp.tapir.codegen.openapi.models.OpenapiModels.OpenapiDocument
import sttp.tapir.codegen.openapi.models.OpenapiSchemaType
import sttp.tapir.codegen.openapi.models.OpenapiSchemaType.{
  OpenapiSchemaArray,
  OpenapiSchemaConstantString,
  OpenapiSchemaEnum,
  OpenapiSchemaMap,
  OpenapiSchemaObject,
  OpenapiSchemaSimpleType
}

class ClassDefinitionGenerator {

  def classDefs(doc: OpenapiDocument, targetScala3: Boolean = false): Option[String] = {
    doc.components
      .map(_.schemas.flatMap {
        case (name, obj: OpenapiSchemaObject) =>
          generateClass(name, obj)
        case (name, obj: OpenapiSchemaEnum) =>
          generateEnum(name, obj, targetScala3)
        case (name, OpenapiSchemaMap(valueSchema, _)) => generateMap(name, valueSchema)
        case (n, x) => throw new NotImplementedError(s"Only objects and enums supported! (for $n found ${x})")
      })
      .map(_.mkString("\n"))
  }

  private[codegen] def generateMap(name: String, valueSchema: OpenapiSchemaType): Seq[String] = {
    val valueSchemaName = valueSchema match {
      case OpenapiSchemaType.OpenapiSchemaBoolean(_) => "Boolean"
    }
    Seq(s"""type $name = Map[String, $valueSchemaName]""")
  }

  // Uses enumeratum for scala 2, but generates scala 3 enums instead where it can
  private[codegen] def generateEnum(name: String, obj: OpenapiSchemaEnum, targetScala3: Boolean): Seq[String] = {
    val hasIllegalChar = ".*[^0-9a-zA-Z$_].*".r
    def toLegalName(s: String): String = if (hasIllegalChar.findFirstIn(s).isDefined) s"`$s`" else s
    val legalName = toLegalName(name)
    if (targetScala3) {
      s"""enum $legalName {
         |  case ${obj.items.map(i => toLegalName(i.value)).mkString(", ")}
         |}
         |implicit lazy val ${legalName}PlainCodec: Codec.PlainCodec[$legalName] =
         |  Codec.string.map($legalName.valueOf(_))(_.toString)
         |implicit lazy val ${legalName}JsonCodec: Codec.JsonCodec[$legalName] =
         |  Codec.json(s => scala.util.Try($legalName.valueOf(s)).fold(t => DecodeResult.Error(s, t), DecodeResult.Value(_)))(_.toString)""".stripMargin :: Nil
    } else {
      val members = obj.items.map { i => s"case object ${toLegalName(i.value)} extends $legalName" }
      s"""|sealed trait $legalName extends EnumEntry
          |object $legalName extends Enum[$legalName] with CirceEnum[$legalName] {
          |  val values = findValues
          |${indent(2)(members.mkString("\n"))}
          |}
          |implicit lazy val ${legalName}PlainCodec: Codec.PlainCodec[$legalName] =
          |  Codec.string.map($legalName.withName(_))(_.entryName)
          |implicit lazy val ${legalName}JsonCodec: Codec.JsonCodec[$legalName] =
          |  Codec.json(s => scala.util.Try($legalName.withName(s)).fold(t => DecodeResult.Error(s, t), DecodeResult.Value(_)))(_.entryName)""".stripMargin :: Nil
    }
  }

  private[codegen] def generateClass(name: String, obj: OpenapiSchemaObject): Seq[String] = {
    def rec(name: String, obj: OpenapiSchemaObject, acc: List[String]): Seq[String] = {
      val innerClasses = obj.properties
        .collect {
          case (propName, st: OpenapiSchemaObject) =>
            val newName = addName(name, propName)
            rec(newName, st, Nil)

          case (propName, OpenapiSchemaMap(st: OpenapiSchemaObject, _)) =>
            val newName = addName(addName(name, propName), "item")
            rec(newName, st, Nil)

          case (propName, OpenapiSchemaArray(st: OpenapiSchemaObject, _)) =>
            val newName = addName(addName(name, propName), "item")
            rec(newName, st, Nil)
        }
        .flatten
        .toList

      val (fieldNames, properties) = obj.properties.map { case (key, schemaType) =>
        val tpe = mapSchemaTypeToType(name, key, obj.required.contains(key), schemaType)
        val fixedKey = fixKey(key)
        fixedKey -> s"$fixedKey: $tpe"
      }.unzip
      val fieldCount = fieldNames.size
      val fieldNamesString = fieldNames.map('"' +: _ :+ '"').mkString(", ")
      val fieldAccessString = fieldNames.map("x." + _).mkString(", ")
      val explicitCodecs = if (fieldCount > 22)
        s"""object $name {
           |  implicit lazy val decode$name: Decoder[$name] = deriveDecoder[$name]
           |  implicit lazy val encode$name: Encoder[$name] = deriveEncoder[$name]
           |}""".stripMargin else
        s"""implicit lazy val decode$name: Decoder[$name] =
           |  Decoder.forProduct$fieldCount($fieldNamesString)($name.apply)
           |implicit lazy val encode$name: Encoder[$name] =
           |  Encoder.forProduct$fieldCount($fieldNamesString)(x =>
           |    ($fieldAccessString)
           |  )""".stripMargin

      s"""|case class $name (
          |${indent(2)(properties.mkString(",\n"))}
          |)
          |$explicitCodecs""".stripMargin :: innerClasses ::: acc
    }

    rec(addName("", name), obj, Nil)
  }

  private def mapSchemaTypeToType(parentName: String, key: String, required: Boolean, schemaType: OpenapiSchemaType): String = {
    val (tpe, optional) = schemaType match {
      case simpleType: OpenapiSchemaSimpleType =>
        mapSchemaSimpleTypeToType(simpleType)

      case objectType: OpenapiSchemaObject =>
        addName(parentName, key) -> objectType.nullable

      case mapType: OpenapiSchemaMap =>
        val innerType = mapSchemaTypeToType(addName(parentName, key), "item", required = true, mapType.items)
        s"Map[String, $innerType]" -> mapType.nullable

      case arrayType: OpenapiSchemaArray =>
        val innerType = mapSchemaTypeToType(addName(parentName, key), "item", required = true, arrayType.items)
        s"Seq[$innerType]" -> arrayType.nullable

      case _ =>
        throw new NotImplementedError(s"We can't serialize some of the properties yet! $parentName $key $schemaType")
    }

    if (optional || !required) s"Option[$tpe]" else tpe
  }

  private def addName(parentName: String, key: String) = parentName + key.replace('_', ' ').replace('-', ' ').capitalize.replace(" ", "")

  private val reservedKeys = scala.reflect.runtime.universe.asInstanceOf[scala.reflect.internal.SymbolTable].nme.keywords.map(_.toString)

  private def fixKey(key: String) = {
    if (reservedKeys.contains(key))
      s"`$key`"
    else
      key
  }
}
