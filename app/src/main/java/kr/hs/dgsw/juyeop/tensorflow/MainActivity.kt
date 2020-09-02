package kr.hs.dgsw.juyeop.tensorflow

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_main.*
import kr.hs.dgsw.juyeop.tensorflow.databinding.ActivityMainBinding
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.Executors

/**
 * mobilenet.tflite
 * - QUANT : false
 * - label = 1000
 *
 * mobilenet_float_v1_224.tflite
 * - QUANT : flase
 * - label = 1001
 *
 * mobilenet_quant_v1_224.tflite
 * - QUANT : true
 * - label = 1001
 */

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private lateinit var classifier: Classifier
    private val executor = Executors.newSingleThreadExecutor()

    private val MODEL_PATH = "model.tflite"
    private val LABEL_PATH = "labels2.txt"
    private val INPUT_SIZE = 224
    private val QUANT = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = this@MainActivity

        observerViewModel()
        initTensorFlowAndLoadModel()
    }

    private fun observerViewModel() {
        with(viewModel) {
            onToggleEvent.observe(this@MainActivity, Observer {
                cameraView.toggleFacing()
            })
            onDetectEvent.observe(this@MainActivity, Observer {
                cameraView.captureImage { cameraKitView, bytes ->
                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)

                    imageView.setImageBitmap(bitmap)

                    val results = classifier.recognizeImage(bitmap)
                    textView.text = results.toString()
                }
            })
        }
    }

    @Throws(Exception::class)
    private fun initTensorFlowAndLoadModel() {
        try {
            classifier = TensorFlowImageClassifier().create(assets, MODEL_PATH, LABEL_PATH, INPUT_SIZE, QUANT)
            makeDetectButtonVisible()
        } catch (e: Exception) {
            throw RuntimeException("TensorFlow 세팅 과정 속 오류 발생", e)
        }
    }

    private fun makeDetectButtonVisible() {
        runOnUiThread { detectButton.visibility = View.VISIBLE }
    }

    override fun onStart() {
        super.onStart()
        cameraView.onStart()
    }
    override fun onResume() {
        super.onResume()
        cameraView.onResume()
    }
    override fun onPause() {
        super.onPause()
        cameraView.onPause()
    }
    override fun onStop() {
        super.onStop()
        cameraView.onStop()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraView.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}