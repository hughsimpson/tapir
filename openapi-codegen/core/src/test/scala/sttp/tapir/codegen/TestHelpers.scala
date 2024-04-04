package sttp.tapir.codegen

import io.circe.Json
import sttp.tapir.codegen.openapi.models.OpenapiComponent
import sttp.tapir.codegen.openapi.models.OpenapiModels._
import sttp.tapir.codegen.openapi.models.OpenapiSchemaType._
import sttp.tapir.codegen.openapi.models.OpenapiSecuritySchemeType.OpenapiSecuritySchemeApiKeyType

import scala.io.Source

object TestHelpers {

  val myBookshopYaml =
    """
      |openapi: 3.1.0
      |info:
      |  title: My Bookshop
      |  version: '1.0'
      |paths:
      |  /books/{genre}/{year}:
      |    parameters:
      |    - name: genre
      |      in: path
      |      schema:
      |        type: string
      |    - $ref: '#/components/parameters/year'
      |    post:
      |      operationId: postBooksGenreYear
      |      parameters:
      |      - name: year
      |        in: path
      |        required: true
      |        schema:
      |          type: integer
      |      - name: limit
      |        in: query
      |        description: Maximum number of books to retrieve
      |        required: true
      |        schema:
      |          type: integer
      |      - name: X-Auth-Token
      |        in: header
      |        required: true
      |        schema:
      |          type: string
      |      requestBody:
      |        description: Book to add
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Book'
      |      responses:
      |        '200':
      |          description: ''
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/Book'
      |      tags:
      |        - Bookshop
      |    get:
      |      operationId: getBooksGenreYear
      |      parameters:
      |      - name: genre
      |        in: path
      |        required: true
      |        schema:
      |          type: string
      |      - $ref: '#/components/parameters/offset'
      |      - name: limit
      |        in: query
      |        description: Maximum number of books to retrieve
      |        required: true
      |        schema:
      |          type: integer
      |      - name: X-Auth-Token
      |        in: header
      |        required: true
      |        schema:
      |          type: string
      |      responses:
      |        '200':
      |          description: ''
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/Book'
      |        default:
      |          description: ''
      |          content:
      |            text/plain:
      |              schema:
      |                type: string
      |      tags:
      |        - Bookshop
      |    delete:
      |      operationId: deleteBooksGenreYear
      |      responses:
      |        '200':
      |          description: 'deletion was successful'
      |        default:
      |          description: 'deletion failed'
      |      tags:
      |        - Bookshop
      |components:
      |  schemas:
      |    Book:
      |      required:
      |      - title
      |      type: object
      |      properties:
      |        title:
      |          type: string
      |  parameters:
      |    offset:
      |      name: offset
      |      in: query
      |      description: Offset at which to start fetching books
      |      required: true
      |      schema:
      |        type: integer
      |    year:
      |      name: year
      |      in: path
      |      required: true
      |      schema:
      |        type: integer
      |""".stripMargin

  val myBookshopDoc = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("My Bookshop", "1.0"),
    Seq(
      OpenapiPath(
        "/books/{genre}/{year}",
        Seq(
          OpenapiPathMethod(
            methodType = "get",
            parameters = Seq(
              Resolved(OpenapiParameter("genre", "path", Some(true), None, OpenapiSchemaString(false))),
              Ref[OpenapiParameter]("#/components/parameters/offset"),
              Resolved(
                OpenapiParameter("limit", "query", Some(true), Some("Maximum number of books to retrieve"), OpenapiSchemaInt(false))
              ),
              Resolved(OpenapiParameter("X-Auth-Token", "header", Some(true), None, OpenapiSchemaString(false)))
            ),
            responses = Seq(
              OpenapiResponse(
                "200",
                "",
                Seq(OpenapiResponseContent("application/json", OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Book"), false)))
              ),
              OpenapiResponse("default", "", Seq(OpenapiResponseContent("text/plain", OpenapiSchemaString(false))))
            ),
            requestBody = None,
            summary = None,
            tags = Some(Seq("Bookshop")),
            operationId = Some("getBooksGenreYear")
          ),
          OpenapiPathMethod(
            methodType = "post",
            parameters = Seq(
              Resolved(OpenapiParameter("year", "path", Some(true), None, OpenapiSchemaInt(false))),
              Resolved(
                OpenapiParameter("limit", "query", Some(true), Some("Maximum number of books to retrieve"), OpenapiSchemaInt(false))
              ),
              Resolved(OpenapiParameter("X-Auth-Token", "header", Some(true), None, OpenapiSchemaString(false)))
            ),
            responses = Seq(
              OpenapiResponse(
                "200",
                "",
                Seq(OpenapiResponseContent("application/json", OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Book"), false)))
              )
            ),
            requestBody = Option(
              OpenapiRequestBody(
                required = true,
                content = Seq(
                  OpenapiRequestBodyContent(
                    "application/json",
                    OpenapiSchemaRef("#/components/schemas/Book")
                  )
                ),
                description = Some("Book to add")
              )
            ),
            summary = None,
            tags = Some(Seq("Bookshop")),
            operationId = Some("postBooksGenreYear")
          ),
          OpenapiPathMethod(
            methodType = "delete",
            parameters = Nil,
            responses = Seq(
              OpenapiResponse("200", "deletion was successful", Nil),
              OpenapiResponse("default", "deletion failed", Nil)
            ),
            requestBody = None,
            summary = None,
            tags = Some(Seq("Bookshop")),
            operationId = Some("deleteBooksGenreYear")
          )
        ),
        parameters = Seq(
          Resolved(OpenapiParameter("genre", "path", None, None, OpenapiSchemaString(false))),
          Ref("#/components/parameters/year")
        )
      )
    ),
    Some(
      OpenapiComponent(
        schemas = Map(
          "Book" -> OpenapiSchemaObject(Map("title" -> OpenapiSchemaField(OpenapiSchemaString(false), None)), Seq("title"), false)
        ),
        securitySchemes = Map.empty,
        parameters = Map(
          "#/components/parameters/offset" ->
            OpenapiParameter("offset", "query", Some(true), Some("Offset at which to start fetching books"), OpenapiSchemaInt(false)),
          "#/components/parameters/year" -> OpenapiParameter("year", "path", Some(true), None, OpenapiSchemaInt(false))
        )
      )
    )
  )

  val generatedBookshopYaml =
    """
      |openapi: 3.1.0
      |info:
      |  title: Generated Bookshop
      |  version: '1.0'
      |paths:
      |  /hello:
      |    get:
      |      operationId: getHello
      |      parameters:
      |      - name: name
      |        in: query
      |        required: true
      |        schema:
      |          type: string
      |      responses:
      |        '200':
      |          description: ''
      |          content:
      |            text/plain:
      |              schema:
      |                type: string
      |        '400':
      |          description: 'Invalid value for: query parameter name'
      |          content:
      |            text/plain:
      |              schema:
      |                type: string
      |  /books/list/all:
      |    get:
      |      operationId: getBooksListAll
      |      responses:
      |        '200':
      |          description: ''
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/Book'
      |components:
      |  schemas:
      |    Author:
      |      required:
      |      - name
      |      type: object
      |      properties:
      |        name:
      |          type: string
      |    Book:
      |      required:
      |      - title
      |      - year
      |      - author
      |      type: object
      |      properties:
      |        title:
      |          type: string
      |        year:
      |          type: integer
      |          format: int32
      |        author:
      |          $ref: '#/components/schemas/Author'
      |""".stripMargin

  val generatedBookshopDoc = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("Generated Bookshop", "1.0"),
    Seq(
      OpenapiPath(
        url = "/hello",
        methods = Seq(
          OpenapiPathMethod(
            methodType = "get",
            parameters = Seq(
              Resolved(OpenapiParameter("name", "query", Some(true), None, OpenapiSchemaString(false)))
            ),
            responses = Seq(
              OpenapiResponse(
                "200",
                "",
                Seq(OpenapiResponseContent("text/plain", OpenapiSchemaString(false)))
              ),
              OpenapiResponse(
                code = "400",
                description = "Invalid value for: query parameter name",
                content = Seq(OpenapiResponseContent("text/plain", OpenapiSchemaString(false)))
              )
            ),
            requestBody = None,
            summary = None,
            tags = None,
            operationId = Some("getHello")
          )
        )
      ),
      OpenapiPath(
        url = "/books/list/all",
        methods = Seq(
          OpenapiPathMethod(
            methodType = "get",
            parameters = Seq(),
            responses = Seq(
              OpenapiResponse(
                code = "200",
                description = "",
                content =
                  List(OpenapiResponseContent("application/json", OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Book"), false)))
              )
            ),
            requestBody = None,
            summary = None,
            tags = None,
            operationId = Some("getBooksListAll")
          )
        )
      )
    ),
    Some(
      OpenapiComponent(
        Map(
          "Author" -> OpenapiSchemaObject(Map("name" -> OpenapiSchemaField(OpenapiSchemaString(false), None)), List("name"), false),
          "Book" -> OpenapiSchemaObject(
            properties = Map(
              "title" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "year" -> OpenapiSchemaField(OpenapiSchemaInt(false), None),
              "author" -> OpenapiSchemaField(OpenapiSchemaRef("#/components/schemas/Author"), None)
            ),
            required = Seq("title", "year", "author"),
            nullable = false
          )
        )
      )
    )
  )

  val helloYaml =
    """openapi: 3.1.0
      |info:
      |  title: hello
      |  version: 1.0.0
      |paths:
      |  /hello:
      |    get:
      |      operationId: getHello
      |      parameters:
      |      - name: name
      |        in: path
      |        required: true
      |        schema:
      |          type: string
      |      responses:
      |        '200':
      |          description: ''
      |          content:
      |            text/plain:
      |              schema:
      |                type: string
      |        '400':
      |          description: 'Invalid value for: query parameter name'
      |          content:
      |            text/plain:
      |              schema:
      |                type: string
      |""".stripMargin

  val helloDocs = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("hello", "1.0.0"),
    Seq(
      OpenapiPath(
        url = "/hello",
        methods = Seq(
          OpenapiPathMethod(
            methodType = "get",
            Seq(
              Resolved(OpenapiParameter("name", "path", Some(true), None, OpenapiSchemaString(false)))
            ),
            responses = Seq(
              OpenapiResponse(
                "200",
                "",
                Seq(OpenapiResponseContent("text/plain", OpenapiSchemaString(false)))
              ),
              OpenapiResponse(
                code = "400",
                description = "Invalid value for: query parameter name",
                content = Seq(OpenapiResponseContent("text/plain", OpenapiSchemaString(false)))
              )
            ),
            requestBody = None,
            summary = None,
            tags = None,
            operationId = Some("getHello")
          )
        )
      )
    ),
    None
  )

  val simpleSecurityYaml =
    """
      |openapi: 3.1.0
      |info:
      |  title: hello
      |  version: '1.0'
      |paths:
      |  /hello:
      |    get:
      |      security:
      |        - basicAuth: []
      |      responses: {}
    """.stripMargin

  val simpleSecurityDocs = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("hello", "1.0"),
    Seq(
      OpenapiPath(
        url = "/hello",
        methods = Seq(
          OpenapiPathMethod(
            methodType = "get",
            parameters = Seq(),
            responses = Seq(),
            requestBody = None,
            security = Seq(Seq("basicAuth"))
          )
        )
      )
    ),
    None
  )

  val complexSecurityYaml =
    """
      |openapi: 3.1.0
      |info:
      |  title: hello
      |  version: '1.0'
      |paths:
      |  /hello:
      |    get:
      |      security:
      |        - bearerAuth: []
      |        - basicAuth: []
      |          apiKeyAuth: []
      |      responses: {}
    """.stripMargin

  val complexSecurityDocs = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("hello", "1.0"),
    Seq(
      OpenapiPath(
        url = "/hello",
        methods = Seq(
          OpenapiPathMethod(
            methodType = "get",
            parameters = Seq(),
            responses = Seq(),
            requestBody = None,
            security = Seq(Seq("bearerAuth"), Seq("basicAuth", "apiKeyAuth"))
          )
        )
      )
    ),
    None
  )

  val enumQueryParamYaml =
    """
      |openapi: 3.1.0
      |info:
      |  title: enum query test
      |  version: '1.0'
      |paths:
      |  /queryTest:
      |    parameters:
      |      - name: test
      |        in: query
      |        required: false
      |        schema:
      |          $ref: '#/components/schemas/Test'
      |    post:
      |      responses: {}
      |
      |components:
      |  schemas:
      |    Test:
      |      title: Test
      |      type: string
      |      enum:
      |        - paperback
      |        - hardback
      |""".stripMargin

  val enumQueryParamDocs = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("enum query test", "1.0"),
    Seq(
      OpenapiPath(
        "/queryTest",
        Seq(
          OpenapiPathMethod(
            methodType = "post",
            parameters = Seq(),
            responses = Seq(),
            requestBody = None,
            summary = None,
            tags = None,
            operationId = None
          )
        ),
        parameters = Seq(
          Resolved(OpenapiParameter("test", "query", None, None, OpenapiSchemaRef("#/components/schemas/Test")))
        )
      )
    ),
    Some(
      OpenapiComponent(
        Map(
          "Test" -> OpenapiSchemaEnum(
            "string",
            Seq(OpenapiSchemaConstantString("paperback"), OpenapiSchemaConstantString("hardback")),
            false
          )
        )
      )
    )
  )

  val withDefaultsYaml =
    """
     |openapi: 3.1.0
     |info:
     |  title: default test
     |  version: '1.0'
     |paths:
     |  /hello:
     |    post:
     |      requestBody:
     |        description: Foo
     |        required: true
     |        content:
     |          application/json:
     |            schema:
     |              $ref: '#/components/schemas/ReqWithDefaults'
     |      responses:
     |        '200':
     |          description: Bar
     |          content:
     |            application/json:
     |              schema:
     |                type: array
     |                items:
     |                  $ref: '#/components/schemas/RespWithDefaults'
     |components:
     |  schemas:
     |    ReqWithDefaults:
     |      required:
     |      - f1
     |      type: object
     |      properties:
     |        f1:
     |          type: string
     |          default: default string
     |        f2:
     |          type: integer
     |          format: int32
     |          default: 1977
     |    RespWithDefaults:
     |      required:
     |      - g2
     |      type: object
     |      properties:
     |        g1:
     |          type: string
     |          format: uuid
     |          default: default string
     |        g2:
     |          type: number
     |          format: float
     |          default: 1977
     |        g3:
     |          $ref: '#/components/schemas/AnEnum'
     |          default: v1
     |        g4:
     |          type: array
     |          items:
     |            $ref: '#/components/schemas/AnEnum'
     |          default: [v1, v2, v3]
     |        sub:
     |          $ref: '#/components/schemas/SubObject'
     |          default:
     |            subsub:
     |              value: hi there
     |              value2: ac8113ed-6105-4f65-a393-e88be2c5d585
     |    AnEnum:
     |      title: AnEnum
     |      type: string
     |      enum:
     |        - v1
     |        - v2
     |        - v3
     |    SubObject:
     |      required:
     |      - subsub
     |      type: object
     |      properties:
     |        subsub:
     |          $ref: '#/components/schemas/SubSubObject'
     |    SubSubObject:
     |      required:
     |      - value
     |      type: object
     |      properties:
     |        value:
     |          type: string
     |        value2:
     |          type: string
     |          format: uuid
     |""".stripMargin

  val withDefaultsDocs = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("default test", "1.0"),
    List(
      OpenapiPath(
        "/hello",
        List(
          OpenapiPathMethod(
            "post",
            List(),
            List(
              OpenapiResponse(
                "200",
                "Bar",
                List(
                  OpenapiResponseContent(
                    "application/json",
                    OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/RespWithDefaults"), false)
                  )
                )
              )
            ),
            Some(
              OpenapiRequestBody(
                true,
                Some("Foo"),
                List(OpenapiRequestBodyContent("application/json", OpenapiSchemaRef("#/components/schemas/ReqWithDefaults")))
              )
            ),
            List(),
            None,
            None,
            None
          )
        ),
        List()
      )
    ),
    Some(
      OpenapiComponent(
        Map(
          "ReqWithDefaults" -> OpenapiSchemaObject(
            Map(
              "f1" -> OpenapiSchemaField(OpenapiSchemaString(false), Some(Json.fromString("default string"))),
              "f2" -> OpenapiSchemaField(OpenapiSchemaInt(false), Some(Json.fromLong(1977)))
            ),
            List("f1"),
            false
          ),
          "RespWithDefaults" -> OpenapiSchemaObject(
            Map(
              "g1" -> OpenapiSchemaField(OpenapiSchemaUUID(false), Some(Json.fromString("default string"))),
              "g2" -> OpenapiSchemaField(OpenapiSchemaFloat(false), Some(Json.fromLong(1977))),
              "g3" -> OpenapiSchemaField(OpenapiSchemaRef("#/components/schemas/AnEnum"), Some(Json.fromString("v1"))),
              "g4" -> OpenapiSchemaField(
                OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/AnEnum"), false),
                Some(Json.fromValues(Vector(Json.fromString("v1"), Json.fromString("v2"), Json.fromString("v3"))))
              ),
              "sub" -> OpenapiSchemaField(
                OpenapiSchemaRef("#/components/schemas/SubObject"),
                Some(
                  Json.fromFields(
                    Map(
                      "subsub" -> Json.fromFields(
                        Map(
                          "value" -> Json.fromString("hi there"),
                          "value2" -> Json.fromString("ac8113ed-6105-4f65-a393-e88be2c5d585")
                        )
                      )
                    )
                  )
                )
              )
            ),
            List("g2"),
            false
          ),
          "AnEnum" -> OpenapiSchemaEnum(
            "string",
            List(OpenapiSchemaConstantString("v1"), OpenapiSchemaConstantString("v2"), OpenapiSchemaConstantString("v3")),
            false
          ),
          "SubObject" -> OpenapiSchemaObject(
            Map("subsub" -> OpenapiSchemaField(OpenapiSchemaRef("#/components/schemas/SubSubObject"), None)),
            List("subsub"),
            false
          ),
          "SubSubObject" -> OpenapiSchemaObject(
            Map(
              "value" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "value2" -> OpenapiSchemaField(OpenapiSchemaUUID(false), None)
            ),
            List("value"),
            false
          )
        ),
        Map(),
        Map()
      )
    )
  )

  val specificationExtensionYaml =
    """
     |openapi: 3.1.0
     |info:
     |  title: hello goodbye
     |  version: '1.0'
     |paths:
     |  /hello:
     |    x-custom-string-extension-on-path-any-type: foobar
     |    x-custom-string-extension-on-path-double-type: 123
     |    x-custom-string-extension-on-path: null
     |    x-custom-list-extension-on-path:
     |      - foo
     |      - bar
     |    x-custom-list-extension-on-path-any-type:
     |      - string
     |    x-custom-map-extension-on-path:
     |      bazkey: bazval
     |      quuxkey:
     |        - quux1
     |        - quux2
     |    post:
     |      responses: {}
     |  /goodbye:
     |    x-custom-string-extension-on-path-any-type: 123
     |    x-custom-string-extension-on-path-double-type: 123.456
     |    x-custom-string-extension-on-path: another string
     |    x-custom-list-extension-on-path: []
     |    x-custom-list-extension-on-path-any-type:
     |      - 123
     |    x-custom-map-extension-on-path: {}
     |    x-custom-map-extension-on-path-single-value-type:
     |      bazkey: bazval
     |      quuxkey: quuxval
     |    delete:
     |      x-custom-string-extension-on-operation: bazquux
     |      x-custom-list-extension-on-operation:
     |        - baz
     |        - quux
     |      x-custom-map-extension-on-operation:
     |        bazkey: bazval
     |        quuxkey:
     |          - quux1
     |          - quux2
     |      responses: {}""".stripMargin

  val specificationExtensionDocs = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("hello goodbye", "1.0"),
    Seq(
      OpenapiPath(
        url = "/hello",
        methods = Seq(OpenapiPathMethod(methodType = "post", parameters = Seq(), responses = Seq(), requestBody = None)),
        specificationExtensions = Map(
          "custom-string-extension-on-path-any-type" -> Json.fromString("foobar"),
          "custom-string-extension-on-path-double-type" -> Json.fromLong(123L),
          "custom-list-extension-on-path" -> Json.fromValues(
            Vector(Json.fromString("foo"), Json.fromString("bar"))
          ),
          "custom-list-extension-on-path-any-type" -> Json.arr(Json.fromString("string")),
          "custom-map-extension-on-path" -> Json.fromFields(
            Map(
              "bazkey" -> Json.fromString("bazval"),
              "quuxkey" -> Json.fromValues(
                Vector(Json.fromString("quux1"), Json.fromString("quux2"))
              )
            )
          )
        )
      ),
      OpenapiPath(
        url = "/goodbye",
        methods = Seq(
          OpenapiPathMethod(
            methodType = "delete",
            parameters = Seq(),
            responses = Seq(),
            requestBody = None,
            specificationExtensions = Map(
              "custom-string-extension-on-operation" -> Json.fromString("bazquux"),
              "custom-list-extension-on-operation" -> Json.fromValues(
                Vector(Json.fromString("baz"), Json.fromString("quux"))
              ),
              "custom-map-extension-on-operation" -> Json.fromFields(
                Map(
                  "bazkey" -> Json.fromString("bazval"),
                  "quuxkey" -> Json.fromValues(
                    Vector(Json.fromString("quux1"), Json.fromString("quux2"))
                  )
                )
              )
            )
          )
        ),
        specificationExtensions = Map(
          "custom-string-extension-on-path-any-type" -> Json.fromLong(123L),
          "custom-string-extension-on-path-double-type" -> Json.fromDouble(123.456d).get,
          "custom-string-extension-on-path" -> Json.fromString("another string"),
          "custom-list-extension-on-path" -> Json.arr(),
          "custom-list-extension-on-path-any-type" -> Json.arr(Json.fromLong(123L)),
          "custom-map-extension-on-path" -> Json.fromFields(Map.empty),
          "custom-map-extension-on-path-single-value-type" -> Json.fromFields(
            Map(
              "bazkey" -> Json.fromString("bazval"),
              "quuxkey" -> Json.fromString("quuxval")
            )
          )
        )
      )
    ),
    None
  )

  val oneOfYaml =
    """
    |openapi: 3.1.0
    |info:
    |  title: oneOf test
    |  version: '1.0'
    |paths:
    |  /hello:
    |    post:
    |      requestBody:
    |        description: Foo
    |        required: true
    |        content:
    |          application/json:
    |            schema:
    |              $ref: '#/components/schemas/ReqWithVariants'
    |      responses:
    |        '200':
    |          description: Bar
    |          content:
    |            application/json:
    |              schema:
    |                type: string
    |components:
    |  schemas:
    |    ReqWithVariants:
    |      title: ReqWithVariants
    |      required:
    |        - type
    |      type: object
    |      properties:
    |        type:
    |          $ref: '#/components/schemas/ReqSubtype'
    |      discriminator:
    |        propertyName: type
    |        mapping:
    |          ReqSubtype1: '#/components/schemas/ReqSubtype1'
    |          ReqSubtype2: '#/components/schemas/ReqSubtype2'
    |          ReqSubtype3: '#/components/schemas/ReqSubtype3'
    |          ReqSubtype4: '#/components/schemas/ReqSubtype4'
    |      oneOf:
    |        - $ref: '#/components/schemas/ReqSubtype1'
    |        - $ref: '#/components/schemas/ReqSubtype2'
    |        - $ref: '#/components/schemas/ReqSubtype3'
    |        - $ref: '#/components/schemas/ReqSubtype4'
    |    ReqSubtype:
    |      title: ReqSubtype
    |      type: string
    |      enum:
    |        - ReqSubtype1
    |        - ReqSubtype2
    |        - ReqSubtype3
    |        - ReqSubtype4
    |    ReqSubtype1:
    |      required:
    |      - foo
    |      type: object
    |      properties:
    |        foo:
    |          type: integer
    |    ReqSubtype2:
    |      required:
    |      - foo
    |      type: object
    |      properties:
    |        foo:
    |          type: string
    |    ReqSubtype3:
    |      required:
    |      - foo
    |      type: object
    |      properties:
    |        foo:
    |          type: string
    |    ReqSubtype4:
    |      required:
    |      - bar
    |      type: object
    |      properties:
    |        bar:
    |          type: string
    |""".stripMargin

  def genOneOfDocs(withDiscriminator: Boolean, withMapping: Boolean) = OpenapiDocument(
    "3.1.0",
    OpenapiInfo("oneOf test", "1.0"),
    List(
      OpenapiPath(
        "/hello",
        List(
          OpenapiPathMethod(
            "post",
            List(),
            List(
              OpenapiResponse(
                "200",
                "Bar",
                List(OpenapiResponseContent("application/json", OpenapiSchemaString(false)))
              )
            ),
            Some(
              OpenapiRequestBody(
                true,
                Some("Foo"),
                List(OpenapiRequestBodyContent("application/json", OpenapiSchemaRef("#/components/schemas/ReqWithVariants")))
              )
            ),
            List(),
            None,
            None,
            None
          )
        ),
        List()
      )
    ),
    Some(
      OpenapiComponent(
        Map(
          "ReqWithVariants" -> OpenapiSchemaOneOf(
            List(
              OpenapiSchemaRef("#/components/schemas/ReqSubtype1"),
              OpenapiSchemaRef("#/components/schemas/ReqSubtype2"),
              OpenapiSchemaRef("#/components/schemas/ReqSubtype3"),
              OpenapiSchemaRef("#/components/schemas/ReqSubtype4")
            ),
            if (withDiscriminator)
              Some(
                Discriminator(
                  "type",
                  if (withMapping)
                    Some(
                      Map(
                        "ReqSubtype1" -> "#/components/schemas/ReqSubtype1",
                        "ReqSubtype2" -> "#/components/schemas/ReqSubtype2",
                        "ReqSubtype3" -> "#/components/schemas/ReqSubtype3",
                        "ReqSubtype4" -> "#/components/schemas/ReqSubtype4"
                      )
                    )
                  else None
                )
              )
            else None
          ),
          "ReqSubtype" -> OpenapiSchemaEnum(
            "string",
            List(
              OpenapiSchemaConstantString("ReqSubtype1"),
              OpenapiSchemaConstantString("ReqSubtype2"),
              OpenapiSchemaConstantString("ReqSubtype3"),
              OpenapiSchemaConstantString("ReqSubtype4")
            ),
            false
          ),
          "ReqSubtype1" -> OpenapiSchemaObject(Map("foo" -> OpenapiSchemaField(OpenapiSchemaInt(false), None)), List("foo"), false),
          "ReqSubtype2" -> OpenapiSchemaObject(Map("foo" -> OpenapiSchemaField(OpenapiSchemaString(false), None)), List("foo"), false),
          "ReqSubtype3" -> OpenapiSchemaObject(Map("foo" -> OpenapiSchemaField(OpenapiSchemaString(false), None)), List("foo"), false),
          "ReqSubtype4" -> OpenapiSchemaObject(Map("bar" -> OpenapiSchemaField(OpenapiSchemaString(false), None)), List("bar"), false)
        ),
        Map(),
        Map()
      )
    )
  )
  val oneOfDocsWithMapping = genOneOfDocs(withDiscriminator = true, withMapping = true)
  val oneOfDocsWithDiscriminatorNoMapping = genOneOfDocs(withDiscriminator = true, withMapping = false)
  val oneOfDocsNoDiscriminator = genOneOfDocs(withDiscriminator = false, withMapping = false)

  def classicPetstoreExampleYaml = Source.fromInputStream(getClass.getResourceAsStream("/petstore.yaml")).getLines().mkString("\n")
  val classicPetstoreExampleDoc = OpenapiDocument(
    "3.0.2",
    OpenapiInfo("Swagger Petstore - OpenAPI 3.0", "1.0.20-SNAPSHOT"),
    List(
      OpenapiPath(
        "/store/order",
        List(
          OpenapiPathMethod(
            "post",
            List(),
            List(
              OpenapiResponse(
                "200",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/Order")))
              ),
              OpenapiResponse("405", "Invalid input", List())
            ),
            Some(
              OpenapiRequestBody(
                false,
                None,
                List(OpenapiRequestBodyContent("application/json", OpenapiSchemaRef("#/components/schemas/Order")))
              )
            ),
            List(),
            Some("Place an order for a pet"),
            Some(List("store")),
            Some("placeOrder"),
            Map("swagger-router-controller" -> Json.fromString("OrderController"))
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/pet/findByTags",
        List(
          OpenapiPathMethod(
            "get",
            List(
              Resolved(
                OpenapiParameter(
                  "tags",
                  "query",
                  Some(false),
                  Some("Tags to filter by"),
                  OpenapiSchemaArray(OpenapiSchemaString(false), false),
                  Some(true)
                )
              )
            ),
            List(
              OpenapiResponse(
                "200",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Pet"), false)))
              ),
              OpenapiResponse("400", "Invalid tag value", List())
            ),
            None,
            List(),
            Some("Finds Pets by tags"),
            Some(List("pet")),
            Some("findPetsByTags"),
            Map()
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/pet",
        List(
          OpenapiPathMethod(
            "put",
            List(),
            List(
              OpenapiResponse(
                "200",
                "Successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/Pet")))
              ),
              OpenapiResponse("400", "Invalid ID supplied", List()),
              OpenapiResponse("404", "Pet not found", List()),
              OpenapiResponse("405", "Validation exception", List())
            ),
            Some(
              OpenapiRequestBody(
                true,
                Some("Update an existent pet in the store"),
                List(OpenapiRequestBodyContent("application/json", OpenapiSchemaRef("#/components/schemas/Pet")))
              )
            ),
            List(),
            Some("Update an existing pet"),
            Some(List("pet")),
            Some("updatePet"),
            Map()
          ),
          OpenapiPathMethod(
            "post",
            List(),
            List(
              OpenapiResponse(
                "200",
                "Successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/Pet")))
              ),
              OpenapiResponse("405", "Invalid input", List())
            ),
            Some(
              OpenapiRequestBody(
                true,
                Some("Create a new pet in the store"),
                List(OpenapiRequestBodyContent("application/json", OpenapiSchemaRef("#/components/schemas/Pet")))
              )
            ),
            List(),
            Some("Add a new pet to the store"),
            Some(List("pet")),
            Some("addPet"),
            Map()
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/store/order/{orderId}",
        List(
          OpenapiPathMethod(
            "get",
            List(
              Resolved(
                OpenapiParameter(
                  "orderId",
                  "path",
                  Some(true),
                  Some("ID of order that needs to be fetched"),
                  OpenapiSchemaLong(false),
                  None
                )
              )
            ),
            List(
              OpenapiResponse(
                "200",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/Order")))
              ),
              OpenapiResponse("400", "Invalid ID supplied", List()),
              OpenapiResponse("404", "Order not found", List())
            ),
            None,
            List(),
            Some("Find purchase order by ID"),
            Some(List("store")),
            Some("getOrderById"),
            Map("swagger-router-controller" -> Json.fromString("OrderController"))
          ),
          OpenapiPathMethod(
            "delete",
            List(
              Resolved(
                OpenapiParameter(
                  "orderId",
                  "path",
                  Some(true),
                  Some("ID of the order that needs to be deleted"),
                  OpenapiSchemaLong(false),
                  None
                )
              )
            ),
            List(OpenapiResponse("400", "Invalid ID supplied", List()), OpenapiResponse("404", "Order not found", List())),
            None,
            List(),
            Some("Delete purchase order by ID"),
            Some(List("store")),
            Some("deleteOrder"),
            Map("swagger-router-controller" -> Json.fromString("OrderController"))
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/user/logout",
        List(
          OpenapiPathMethod(
            "get",
            List(),
            List(OpenapiResponse("default", "successful operation", List())),
            None,
            List(),
            Some("Logs out current logged in user session"),
            Some(List("user")),
            Some("logoutUser"),
            Map()
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/store/inventory",
        List(
          OpenapiPathMethod(
            "get",
            List(),
            List(
              OpenapiResponse(
                "200",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaMap(OpenapiSchemaInt(false), false)))
              )
            ),
            None,
            List(Vector("api_key")),
            Some("Returns pet inventories by status"),
            Some(List("store")),
            Some("getInventory"),
            Map("swagger-router-controller" -> Json.fromString("OrderController"))
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/user/createWithList",
        List(
          OpenapiPathMethod(
            "post",
            List(),
            List(
              OpenapiResponse(
                "200",
                "Successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/User")))
              ),
              OpenapiResponse("default", "successful operation", List())
            ),
            Some(
              OpenapiRequestBody(
                false,
                None,
                List(
                  OpenapiRequestBodyContent("application/json", OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/User"), false))
                )
              )
            ),
            List(),
            Some("Creates list of users with given input array"),
            Some(List("user")),
            Some("createUsersWithListInput"),
            Map("swagger-router-controller" -> Json.fromString("UserController"))
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/user/{username}",
        List(
          OpenapiPathMethod(
            "get",
            List(
              Resolved(
                OpenapiParameter(
                  "username",
                  "path",
                  Some(true),
                  Some("The name that needs to be fetched. Use user1 for testing. "),
                  OpenapiSchemaString(false),
                  None
                )
              )
            ),
            List(
              OpenapiResponse(
                "200",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/User")))
              ),
              OpenapiResponse("400", "Invalid username supplied", List()),
              OpenapiResponse("404", "User not found", List())
            ),
            None,
            List(),
            Some("Get user by user name"),
            Some(List("user")),
            Some("getUserByName"),
            Map()
          ),
          OpenapiPathMethod(
            "put",
            List(
              Resolved(
                OpenapiParameter("username", "path", Some(true), Some("name that needs to be updated"), OpenapiSchemaString(false), None)
              )
            ),
            List(OpenapiResponse("default", "successful operation", List())),
            Some(
              OpenapiRequestBody(
                false,
                Some("Update an existent user in the store"),
                List(OpenapiRequestBodyContent("application/json", OpenapiSchemaRef("#/components/schemas/User")))
              )
            ),
            List(),
            Some("Update user"),
            Some(List("user")),
            Some("updateUser"),
            Map("swagger-router-controller" -> Json.fromString("UserController"))
          ),
          OpenapiPathMethod(
            "delete",
            List(
              Resolved(
                OpenapiParameter(
                  "username",
                  "path",
                  Some(true),
                  Some("The name that needs to be deleted"),
                  OpenapiSchemaString(false),
                  None
                )
              )
            ),
            List(OpenapiResponse("400", "Invalid username supplied", List()), OpenapiResponse("404", "User not found", List())),
            None,
            List(),
            Some("Delete user"),
            Some(List("user")),
            Some("deleteUser"),
            Map()
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/user/login",
        List(
          OpenapiPathMethod(
            "get",
            List(
              Resolved(
                OpenapiParameter("username", "query", Some(false), Some("The user name for login"), OpenapiSchemaString(false), None)
              ),
              Resolved(
                OpenapiParameter(
                  "password",
                  "query",
                  Some(false),
                  Some("The password for login in clear text"),
                  OpenapiSchemaString(false),
                  None
                )
              )
            ),
            List(
              OpenapiResponse("200", "successful operation", List(OpenapiResponseContent("application/json", OpenapiSchemaString(false)))),
              OpenapiResponse("400", "Invalid username/password supplied", List())
            ),
            None,
            List(),
            Some("Logs user into the system"),
            Some(List("user")),
            Some("loginUser"),
            Map()
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/user",
        List(
          OpenapiPathMethod(
            "post",
            List(),
            List(
              OpenapiResponse(
                "default",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/User")))
              )
            ),
            Some(
              OpenapiRequestBody(
                false,
                Some("Created user object"),
                List(OpenapiRequestBodyContent("application/json", OpenapiSchemaRef("#/components/schemas/User")))
              )
            ),
            List(),
            Some("Create user"),
            Some(List("user")),
            Some("createUser"),
            Map()
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/pet/{petId}",
        List(
          OpenapiPathMethod(
            "get",
            List(Resolved(OpenapiParameter("petId", "path", Some(true), Some("ID of pet to return"), OpenapiSchemaLong(false), None))),
            List(
              OpenapiResponse(
                "200",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/Pet")))
              ),
              OpenapiResponse("400", "Invalid ID supplied", List()),
              OpenapiResponse("404", "Pet not found", List())
            ),
            None,
            List(Vector("api_key")),
            Some("Find pet by ID"),
            Some(List("pet")),
            Some("getPetById"),
            Map()
          ),
          OpenapiPathMethod(
            "post",
            List(
              Resolved(
                OpenapiParameter("petId", "path", Some(true), Some("ID of pet that needs to be updated"), OpenapiSchemaLong(false), None)
              ),
              Resolved(
                OpenapiParameter("name", "query", None, Some("Name of pet that needs to be updated"), OpenapiSchemaString(false), None)
              ),
              Resolved(
                OpenapiParameter("status", "query", None, Some("Status of pet that needs to be updated"), OpenapiSchemaString(false), None)
              )
            ),
            List(OpenapiResponse("405", "Invalid input", List())),
            None,
            List(),
            Some("Updates a pet in the store with form data"),
            Some(List("pet")),
            Some("updatePetWithForm"),
            Map()
          ),
          OpenapiPathMethod(
            "delete",
            List(
              Resolved(OpenapiParameter("api_key", "header", Some(false), Some(""), OpenapiSchemaString(false), None)),
              Resolved(OpenapiParameter("petId", "path", Some(true), Some("Pet id to delete"), OpenapiSchemaLong(false), None))
            ),
            List(OpenapiResponse("400", "Invalid pet value", List())),
            None,
            List(),
            Some("Deletes a pet"),
            Some(List("pet")),
            Some("deletePet"),
            Map()
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/pet/{petId}/uploadImage",
        List(
          OpenapiPathMethod(
            "post",
            List(
              Resolved(OpenapiParameter("petId", "path", Some(true), Some("ID of pet to update"), OpenapiSchemaLong(false), None)),
              Resolved(
                OpenapiParameter("additionalMetadata", "query", Some(false), Some("Additional Metadata"), OpenapiSchemaString(false), None)
              )
            ),
            List(
              OpenapiResponse(
                "200",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaRef("#/components/schemas/ApiResponse")))
              )
            ),
            Some(OpenapiRequestBody(false, None, List(OpenapiRequestBodyContent("application/octet-stream", OpenapiSchemaBinary(false))))),
            List(),
            Some("uploads an image"),
            Some(List("pet")),
            Some("uploadFile"),
            Map()
          )
        ),
        List(),
        Map()
      ),
      OpenapiPath(
        "/pet/findByStatus",
        List(
          OpenapiPathMethod(
            "get",
            List(
              Resolved(
                OpenapiParameter(
                  "status",
                  "query",
                  Some(false),
                  Some("Status values that need to be considered for filter"),
                  OpenapiSchemaEnum(
                    "string",
                    List(
                      OpenapiSchemaConstantString("available"),
                      OpenapiSchemaConstantString("pending"),
                      OpenapiSchemaConstantString("sold")
                    ),
                    false
                  ),
                  Some(true)
                )
              )
            ),
            List(
              OpenapiResponse(
                "200",
                "successful operation",
                List(OpenapiResponseContent("application/json", OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Pet"), false)))
              ),
              OpenapiResponse("400", "Invalid status value", List())
            ),
            None,
            List(),
            Some("Finds Pets by status"),
            Some(List("pet")),
            Some("findPetsByStatus"),
            Map()
          )
        ),
        List(),
        Map()
      )
    ),
    Some(
      OpenapiComponent(
        Map(
          "Pet" -> OpenapiSchemaObject(
            Map(
              "name" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "tags" -> OpenapiSchemaField(OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Tag"), false), None),
              "photoUrls" -> OpenapiSchemaField(OpenapiSchemaArray(OpenapiSchemaString(false), false), None),
              "id" -> OpenapiSchemaField(OpenapiSchemaLong(false), None),
              "status" -> OpenapiSchemaField(
                OpenapiSchemaEnum(
                  "string",
                  List(
                    OpenapiSchemaConstantString("available"),
                    OpenapiSchemaConstantString("pending"),
                    OpenapiSchemaConstantString("sold")
                  ),
                  false
                ),
                None
              ),
              "category" -> OpenapiSchemaField(OpenapiSchemaRef("#/components/schemas/Category"), None)
            ),
            List("name", "photoUrls"),
            false
          ),
          "Category" -> OpenapiSchemaObject(
            Map("id" -> OpenapiSchemaField(OpenapiSchemaLong(false), None), "name" -> OpenapiSchemaField(OpenapiSchemaString(false), None)),
            List(),
            false
          ),
          "Address" -> OpenapiSchemaObject(
            Map(
              "street" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "city" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "state" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "zip" -> OpenapiSchemaField(OpenapiSchemaString(false), None)
            ),
            List(),
            false
          ),
          "Tag" -> OpenapiSchemaObject(
            Map("id" -> OpenapiSchemaField(OpenapiSchemaLong(false), None), "name" -> OpenapiSchemaField(OpenapiSchemaString(false), None)),
            List(),
            false
          ),
          "Customer" -> OpenapiSchemaObject(
            Map(
              "id" -> OpenapiSchemaField(OpenapiSchemaLong(false), None),
              "username" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "address" -> OpenapiSchemaField(OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Address"), false), None)
            ),
            List(),
            false
          ),
          "User" -> OpenapiSchemaObject(
            Map(
              "email" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "username" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "userStatus" -> OpenapiSchemaField(OpenapiSchemaInt(false), None),
              "lastName" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "firstName" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "id" -> OpenapiSchemaField(OpenapiSchemaLong(false), None),
              "phone" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "password" -> OpenapiSchemaField(OpenapiSchemaString(false), None)
            ),
            List(),
            false
          ),
          "Order" -> OpenapiSchemaObject(
            Map(
              "shipDate" -> OpenapiSchemaField(OpenapiSchemaDateTime(false), None),
              "quantity" -> OpenapiSchemaField(OpenapiSchemaInt(false), None),
              "petId" -> OpenapiSchemaField(OpenapiSchemaLong(false), None),
              "id" -> OpenapiSchemaField(OpenapiSchemaLong(false), None),
              "complete" -> OpenapiSchemaField(OpenapiSchemaBoolean(false), None),
              "status" -> OpenapiSchemaField(
                OpenapiSchemaEnum(
                  "string",
                  List(
                    OpenapiSchemaConstantString("placed"),
                    OpenapiSchemaConstantString("approved"),
                    OpenapiSchemaConstantString("delivered")
                  ),
                  false
                ),
                None
              )
            ),
            List(),
            false
          ),
          "ApiResponse" -> OpenapiSchemaObject(
            Map(
              "code" -> OpenapiSchemaField(OpenapiSchemaInt(false), None),
              "type" -> OpenapiSchemaField(OpenapiSchemaString(false), None),
              "message" -> OpenapiSchemaField(OpenapiSchemaString(false), None)
            ),
            List(),
            false
          )
        ),
        Map("api_key" -> OpenapiSecuritySchemeApiKeyType("header", "api_key")),
        Map()
      )
    )
  )

}
