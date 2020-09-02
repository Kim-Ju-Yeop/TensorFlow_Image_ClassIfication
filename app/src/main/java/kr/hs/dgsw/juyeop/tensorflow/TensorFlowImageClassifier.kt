package kr.hs.dgsw.juyeop.tensorflow

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.experimental.and

class TensorFlowImageClassifier : Classifier {

    private val MAX_RESULTS = 3
    private val THRESHOLD = 0.1f

    private val BATCH_SIZE = 1
    private val PIXEL_SIZE = 3
    private val IMAGE_MEAN = 128
    private val IMAGE_STD = 128.0f

    private lateinit var interpreter: Interpreter
    private lateinit var labelList: List<String>
    private var inputSize = 0
    private var quant = false

    @Throws(IOException::class)
    fun create(assetManager: AssetManager, modelPath: String, labelPath: String, inputSize: Int, quant: Boolean) : Classifier {
        val classifier = TensorFlowImageClassifier()
        classifier.interpreter = Interpreter(classifier.loadModelFile(assetManager, modelPath), Interpreter.Options())
        classifier.labelList = classifier.loadLabelList(assetManager, labelPath)
        classifier.inputSize = inputSize
        classifier.quant = quant

        return classifier
    }

    // 학습 모델 파일을 불러오는 역할 (mobilenet.tflite)
    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String) : MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)

        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // 학습 모델 라벨을 불러오는 역할 (labels.txt)
    @Throws(IOException::class)
    private fun loadLabelList(assetManager: AssetManager, labelPath: String) : List<String> {
        val labelList = ArrayList<String>()
        val bufferReader = BufferedReader(InputStreamReader(assetManager.open(labelPath)))

        while (true) {
            val line = bufferReader.readLine()
            if (line != null) labelList.add(line)
            else break
        }

        bufferReader.close()
        return labelList
    }

    // 이미지 인식을 진행한다.
    override fun recognizeImage(bitmap: Bitmap): List<Classifier.Recognition> {
        val byteBuffer = convertBitmapToByteBuffer(bitmap)

        return if (quant) {
            val result = Array(1) { ByteArray(labelList.size) }
            interpreter.run(byteBuffer, result)
            getSortedResultByte(result)
        } else {
            val result = Array(1) { FloatArray(labelList.size) }
            interpreter.run(byteBuffer, result)
            getSortResultFloat(result)
        }
    }

    // Bitmap 파일을 ByteBuffer로 변환한다.
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer? {
        val byteBuffer: ByteBuffer

        byteBuffer = if (quant) {
            ByteBuffer.allocateDirect(BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE)
        } else {
            ByteBuffer.allocateDirect(4 * BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE)
        }
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val `val` = intValues[pixel++]
                if (quant) {
                    byteBuffer.put((`val` shr 16 and 0xFF).toByte())
                    byteBuffer.put((`val` shr 8 and 0xFF).toByte())
                    byteBuffer.put((`val` and 0xFF).toByte())
                } else {
                    byteBuffer.putFloat(((`val` shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    byteBuffer.putFloat(((`val` shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    byteBuffer.putFloat(((`val` and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
        return byteBuffer
    }

    @SuppressLint("DefaultLocale")
    private fun getSortedResultByte(labelProbArray: Array<ByteArray>): List<Classifier.Recognition> {
        val priorityQueue = PriorityQueue<Classifier.Recognition>(MAX_RESULTS, object : Comparator<Classifier.Recognition> {
            override fun compare(p0: Classifier.Recognition?, p1: Classifier.Recognition?): Int {
                return java.lang.Float.compare(p1!!.confidence, p0!!.confidence)
            }
        })

        for (i in labelList.indices) {
            val confidence = (labelProbArray[0][i] and 0xff.toByte()) / 255.0f
            if (confidence > THRESHOLD) {
                priorityQueue.add(Classifier.Recognition("" + i, if (labelList.size > i) labelList[i] else "unknown", confidence))
            }
        }

        val recognitions = ArrayList<Classifier.Recognition>()
        val recognitionsSize = Math.min(priorityQueue.size, MAX_RESULTS)

        for (i in 0..recognitionsSize) {
            recognitions.add(priorityQueue.poll())
        }

        return recognitions
    }

    @SuppressLint("DefaultLocale")
    private fun getSortResultFloat(labelProbArray: Array<FloatArray>): List<Classifier.Recognition> {
        val priorityQueue = PriorityQueue<Classifier.Recognition>(MAX_RESULTS, object : Comparator<Classifier.Recognition> {
            override fun compare(p0: Classifier.Recognition?, p1: Classifier.Recognition?): Int {
                return java.lang.Float.compare(p1!!.confidence, p0!!.confidence)
            }
        })

        for (i in labelList.indices) {
            val confidence = labelProbArray[0][i]
            if (confidence > THRESHOLD) {
                priorityQueue.add(Classifier.Recognition("" + i, if (labelList.size > i) labelList[i] else "unknown", confidence))
            }
        }

        val recognitions = ArrayList<Classifier.Recognition>()
        val recognitionsSize = Math.min(priorityQueue.size, MAX_RESULTS)

        for (i in 0..recognitionsSize) {
            recognitions.add(priorityQueue.poll())
        }

        return recognitions
    }

    override fun close() {
        interpreter.close()
    }
}