package com.example

import io.ktor.client.request.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.max


class ScoreResponse(val status:String, val message:String, val results:List<String> )


class SequenceChecker(){
    val sequenceErrorUrl = "http://localhost:2443/checkSequenceErrors"

    suspend fun generateScoreResponse(sequences: ArrayList<String>, checkSequencesViaHttp:Boolean = false): ScoreResponse {
        val scoreResponse: ScoreResponse
        coroutineScope {
            val asyncRequests = sequences.map {
                async {
                    if (checkSequencesViaHttp) client.get<ArrayList<String>>("$sequenceErrorUrl/$it")
                    else getSequenceErrors(it)
                }
            }

            val responses = asyncRequests.map {
                val errors = it.await().joinToString("\n")
                if (errors.isEmpty()) "valid"
                else "Invalid: $errors"
            }
            scoreResponse = ScoreResponse("Complete", "Finished successfully", responses)
        }
        return scoreResponse
    }
    // Can have multiple errors in a single sequence, display all to user!

    val acceptableChars = listOf('A','T','G','C').toHashSet()

    fun getSequenceErrors(string: String): ArrayList<String> {
        val errors = ArrayList<String>()

        if (string.length > 5000) errors += "Sequence too long"
        else if (string.length < 300) errors += "Sequence too short"

        val unacceptableCharactersInString = string.toHashSet().filterNot { it in acceptableChars }
        if (unacceptableCharactersInString.isNotEmpty())
            errors += "Unacceptable characters encountered: " + unacceptableCharactersInString.joinToString()

        val gcRatio = getGcRatio(string)
        if (gcRatio < 0.25f)
            errors += "GC Ratio smaller than 25%!"
        else if (gcRatio > 0.65f)
            errors += "GC Ratio larger than 65%!"

        if (isSubsequenceGcRatioDeltaTooLarge(string))
            errors += "The GC ratio between chunks is too large!"

        return errors
    }

    fun getGcRatio(string:String): Float {
        val GorC = string.count { it == 'G' || it == 'C' }.toFloat()
        return GorC / string.length
    }

    fun isSubsequenceGcRatioDeltaTooLarge(string:String): Boolean {
        val chunks = string.chunked(100)
        var minGcRatio = 1f // start on top, go down gradually
        var maxGcRatio = 0f // start down low, go up gradually
        for (chunk in chunks) {
            val chunkRatio = getGcRatio(chunk)
            if (minGcRatio > chunkRatio) minGcRatio = chunkRatio
            if (maxGcRatio < chunkRatio) maxGcRatio = chunkRatio
        }
        println("Max GC ratio: $maxGcRatio, minGcRation: $minGcRatio")
        return maxGcRatio - minGcRatio > 0.52f
    }
}