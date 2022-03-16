package nl.tudelft.trustchain.atomicswap.ui.swap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SwapViewModel : ViewModel() {

    val createSwapOfferEnabled = MutableLiveData(false)

    fun setSwapOfferEnabled(enabled: Boolean) {
        createSwapOfferEnabled.postValue(enabled)
    }

}
