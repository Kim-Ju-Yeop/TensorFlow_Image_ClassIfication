package kr.hs.dgsw.juyeop.tensorflow

import android.graphics.Bitmap

interface Classifier {

    fun recognizeImage(bitmap: Bitmap) : List<Recognition>
    fun close()

    class Recognition (private val id: String, private val title: String, val confidence: Float) {
        override fun toString(): String {
            var resultString = ""
            resultString += "[${id}]"
            resultString += " $title"
            resultString += String.format("(%.1f%%) ", confidence * 100.0f)

            return resultString.trim()
        }
    }
}