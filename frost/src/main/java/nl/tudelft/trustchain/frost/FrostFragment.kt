package nl.tudelft.trustchain.frost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.NativeSecp256k1
import kotlinx.android.synthetic.main.fragment_frost.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.frost.databinding.FragmentFrostBinding


class FrostFragment : BaseFragment(R.layout.fragment_frost) {
    private val binding by viewBinding(FragmentFrostBinding::bind)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initClickListeners()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_frost, container, false)
    }

    private fun changeText(textView: TextView, text: String){
        textView.text = text
    }

    private fun initClickListeners() {

        button1.setOnClickListener {
            changeText(text_button_1, "")
            changeText(text_button_1, NativeSecp256k1.a())
        }
        button2.setOnClickListener {
            changeText(text_button_2, "")
            changeText(text_button_2, NativeSecp256k1.a())
        }
        button3.setOnClickListener {
            changeText(text_button_3, "")
            changeText(text_button_3, NativeSecp256k1.a())
        }
        button4.setOnClickListener {
            changeText(text_button_4, "")
            changeText(text_button_4, NativeSecp256k1.a())
        }
    }
}
