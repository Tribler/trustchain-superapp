package nl.tudelft.trustchain.eurotoken.ui.exchange

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.fragment_exchange.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity
import org.json.JSONObject


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ExchangeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExchangeFragment : BaseFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!)
    }

    private fun getEuroTokenCommunity(): EuroTokenCommunity {
        return getIpv8().getOverlay() ?: throw java.lang.IllegalStateException("EuroTokenCommunity is not configured")
    }

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        class ConnectionData(json: String) : JSONObject(json) {
            val payment_id = this.optString("payment_id")
            val public_key = this.optString("public_key")
            val ip = this.optString("ip")
            val port = this.optInt("port")
            val amount = this.optLong("amount", -1L)
        }
        qrCodeUtils.parseActivityResult(requestCode, resultCode, data)?.let{
            val connectionData = ConnectionData(it)
            Toast.makeText(requireContext(), connectionData.ip, Toast.LENGTH_LONG).show()
            if (connectionData.amount == -1L){
                getEuroTokenCommunity().connectToGateway(connectionData.public_key, connectionData.ip, connectionData.port, connectionData.payment_id )
                Toast.makeText(requireContext(), "Sending message", Toast.LENGTH_LONG).show()
            } else {
                //getEuroTokenCommunity().connectToGateway(connectionData.public_key, connectionData.ip, connectionData.port, connectionData.payment_id )
                transactionRepository.sendDestroyProposal(connectionData.public_key.hexToBytes(), connectionData.ip, connectionData.port, connectionData.payment_id, connectionData.amount )
                Toast.makeText(requireContext(), "Payment sent", Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
        return
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_connect_gateway.setOnClickListener {
            qrCodeUtils.startQRScanner(this)
        }
        btnDestroy.setOnClickListener {
            qrCodeUtils.startQRScanner(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exchange, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ExchangeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ExchangeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
