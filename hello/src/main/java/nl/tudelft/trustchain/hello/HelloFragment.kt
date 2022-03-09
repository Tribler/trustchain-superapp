package nl.tudelft.trustchain.hello

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_hello.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.hello.databinding.FragmentHelloBinding


class HelloFragment : BaseFragment(R.layout.fragment_hello) {
    private val binding by viewBinding(FragmentHelloBinding::bind)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        initClickListeners()
//        sample_text.text = stringFromJNI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)

        // Example of a call to a native method
//
    }
//
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
        return inflater.inflate(R.layout.fragment_hello, container, false)
    }
//
//    private fun initClickListeners() {
//        val walletManager = WalletManagerAndroid.getInstance()
//        button.setOnClickListener {
//            copyToClipboard(walletManager.protocolAddress().toString())
//        }
}
