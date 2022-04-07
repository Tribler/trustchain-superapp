package nl.tudelft.trustchain.frost

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_frost.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.frost.databinding.FragmentFrostBinding


class FrostFragment : BaseFragment(R.layout.fragment_frost) {
    private val binding by viewBinding(FragmentFrostBinding::bind)

    private fun getFrostCommunity(): FrostCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("FROSTCommunity is not configured")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initClickListeners()
        sample_text.text = "potato"
        val new_text = FrostCpp.stringFromJNI()
        sample_text.text = new_text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)

        // Example of a call to a native method
//
    }

//    /**
//     * A native method that is implemented by the 'hello_cmake' native library,
//     * which is packaged with this application.
//     */
//    external fun stringFromJNI(): String
//
//    companion object {
//        // Used to load the 'hello_cmake' library on application startup.
//        init {
//            System.loadLibrary("hello_cmake")
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_frost, container, false)
    }
//
    private fun writeToFile(filePath: String, text: String){
    this.context?.openFileOutput(filePath, Context.MODE_PRIVATE).use {
        it?.write(text.toByteArray())
        Log.i("FROST", "Write: $text to $filePath")
    }
}

    private fun readFile(filePath: String){
        this.context?.openFileInput(filePath).use { stream ->
            val text = stream?.bufferedReader().use {
                it?.readText()
            }
            Log.i("FROST", "Read: $text from $filePath")
        }
    }
    private fun initClickListeners() {
        button.setOnClickListener {
            // TODO: add FROST JNI
            Log.i("FROST", "Key distribution started")

            val message = "hello"

            writeToFile("key_share.txt", message)
            readFile("key_share.txt")

            getFrostCommunity().distributeKey(message.toByteArray())
        }
    }
}
