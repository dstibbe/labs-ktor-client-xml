package nl.dstibbe.labs.ktor

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun `can convert xml`() {
        runBlocking {
            val client = HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/" -> respond(
                                    content = """
                                    <?xml version="1.0"?>
                                    <CAT>
                                      <name>Izzy</name>
                                      <breed>Siamese</breed>
                                      <children>
                                            <child>
                                                <name>A</name>
                                            </child>
                                            <child>
                                                <name>B</name>
                                            </child>
                                      </children>
                                      <age>6</age>
                                    </CAT>
                                """.trimIndent(),
                                    headers = headersOf("Content-Type", "application/xml")
                            )
                            else -> respond("Not Found ${request.url.encodedPath}", HttpStatusCode.NotFound)
                        }
                    }
                }
                expectSuccess = false
            }

            val mapper = XmlMapper().registerModule(KotlinModule())

            assertEquals(MyDto(
                    name = "Izzy",
                    breed = "Siamese",
                    age = 6,
                    children = listOf(
                            Child(name = "A"),
                            Child(name = "B")
                    )
            ), mapper.readValue(client.get<String>("/")))
        }
    }

    @Test
    fun `can retrieve xml using the jsonfeature`() {
        runBlocking {
            val client = HttpClient(MockEngine) {
                install(JsonFeature) {
                    serializer = JacksonSerializer(jackson = XmlMapper().registerModule(KotlinModule()))
                    accept(ContentType.Application.Xml)
                }
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/" -> respond(
                                    content = """
                                    <?xml version="1.0"?>
                                    <CAT>
                                      <name>Izzy</name>
                                      <breed>Siamese</breed>
                                      <age>6</age>
                                      <children>
                                            <child>
                                                <name>A</name>
                                            </child>
                                            <child>
                                                <name>B</name>
                                            </child>
                                      </children>
                                    </CAT>
                                """.trimIndent(),
                                    headers = headersOf("Content-Type", "application/xml")
                            )
                            else -> respond("Not Found ${request.url.encodedPath}", HttpStatusCode.NotFound)
                        }
                    }
                }
                expectSuccess = false
            }
            assertEquals(MyDto(
                    name = "Izzy",
                    breed = "Siamese",
                    age = 6,
                    children = listOf(
                        Child(name = "A"),
                        Child(name = "B")
                    )
            ), client.get("/"))
        }
    }

    @Test
    fun `can retrieve json`() {
        runBlocking {
            val client = HttpClient(MockEngine) {
                install(JsonFeature) {
                    serializer = JacksonSerializer()
                }
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/" -> respond(
                                    content = """
                                    {
                                      "name" : "Izzy",
                                      "breed" : "Siamese",
                                      "age" : 6,
                                      "children" : []
                                    }
                                """.trimIndent(),
                                    headers = headersOf("Content-Type", "application/json")
                            )
                            else -> respond("Not Found ${request.url.encodedPath}", HttpStatusCode.NotFound)
                        }
                    }
                }
                expectSuccess = false
            }
            assertEquals(MyDto(
                    name = "Izzy",
                    breed = "Siamese",
                    age = 6,
                    children = emptyList()
            ), client.get<MyDto>("/"))
        }
    }
}

data class MyDto(
        val name: String,
        val breed: String,
        val age: Int,
        val children: List<Child>
)

data class Child(
        val name: String
)
