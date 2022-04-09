package nl.tudelft.trustchain.frost

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import bitcoin.NativeSecp256k1
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
        writeToFile(this.context, "acks.txt", "")
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

    private fun sayHelloToCommunity(){
        Log.i("FROST", "Key distribution started")

        val message = "hello"

        writeToFile(this.context, "key_share.txt", message)
        readFile(this.context,"key_share.txt")

        getFrostCommunity().distributeKey(message.toByteArray())
    }

    private fun initClickListeners() {

        button1.setOnClickListener {
            changeText(text_button_1, "Press \"REFRESH\" to check received acks")
//            changeText(text_button_1, NativeSecp256k1.a())
            sayHelloToCommunity()
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
        refresh.setOnClickListener {
            val acks = readFile(this.context, "acks.txt")
            var text = "$acks"
            Log.i("FROST", text)
            changeText(text_refresh, "Received acks: \n $text")
        }
    }
}
