package kr.hs.dgsw.juyeop.tensorflow

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    val onToggleEvent = SingleLiveEvent<Unit>()
    val onDetectEvent = SingleLiveEvent<Unit>()

    fun toggleEvent() {
        onToggleEvent.call()
    }

    fun detectEvent() {
        onDetectEvent.call()
    }
}