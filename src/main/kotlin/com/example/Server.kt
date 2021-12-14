package com.example

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking

object Server {

    @JvmStatic
    fun main(arg: Array<String>) {

        val sequenceChecker = SequenceChecker()

        embeddedServer(Netty, port = 2443) {

            install(ContentNegotiation) {
                gson()
            }
            routing {

                post("/scoreDna") {
                    val sequences: ArrayList<String>
                    try {
                        val body = call.receiveText()
                        sequences = Gson().fromJson(body, ArrayList<String>()::class.java)
//                        sequences = call.receive<ArrayList<String>>() //- this should parse json automatically but doesn't? Can't figure out why
                    } catch (ex:Exception){
                        // Should this return text or a result object?
                        call.respondText("Invalid JSON for sequence input", status = HttpStatusCode.BadRequest)
                        return@post
                    }

                    val scoreResponse = sequenceChecker.generateScoreResponse(sequences,true)

                    call.respond(HttpStatusCode.OK, scoreResponse)
                }

                get("/checkSequenceErrors/{sequence}") {
                    val sequence = call.parameters["sequence"]
                    if (sequence == null) {
                        call.respondText("No sequence inputted", status = HttpStatusCode.BadRequest)
                        return@get
                    }
                    val sequenceErrors = sequenceChecker.getSequenceErrors(sequence)
                    call.respond(HttpStatusCode.OK, sequenceErrors)
                }
            }

        }.start(wait = false) // turn to true for long running - currently false so we can run tests

        // run tests!
        runBlocking {
            try {
//                val checkSequence = client.get<ArrayList<String>>(
//                    "http://localhost:2443/checkSequenceErrors/" + "A".repeat(200)
//                )
//                println("Sequence test errors: " + Gson().toJson(checkSequence))


                val okSequence = "ATGC".repeat(100)

                val gcRatioDeltaTooLarge = "AT".repeat(100) + "GC".repeat(100)

                val response = client.post<ScoreResponse>("http://localhost:2443/scoreDna") {
                    body = "[\"AAAAABBBB\",\"$okSequence\", \"$gcRatioDeltaTooLarge\" ]"
                }
                println("Score response:" + Gson().toJson(response.results))
            } catch (ex: Exception) {
                println(ex.message)
            }
        }

    }
}


val client = HttpClient(CIO) {
    install(JsonFeature){
        serializer = GsonSerializer()
    }
}
