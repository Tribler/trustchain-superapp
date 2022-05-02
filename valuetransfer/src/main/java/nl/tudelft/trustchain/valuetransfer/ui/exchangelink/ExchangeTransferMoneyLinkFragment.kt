package nl.tudelft.trustchain.valuetransfer.ui.exchangelink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentExchangeTransferMoneyLinkBinding
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.util.SecurityUtil
import org.json.JSONObject

class ExchangeTransferMoneyLinkFragment : VTFragment(R.layout.fragment_exchange_transfer_money_link) {
    private val binding by viewBinding(FragmentExchangeTransferMoneyLinkBinding::bind)
    private var ReceiverName = ""
    private var ReceiverPublic = ""
    private var Amount = ""
    private var Message: String? = null
    private var E2T = false
    private var Host = ""
    private var Port = ""
    private var PaymentId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exchange_transfer_money_link, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun initView() {
        parentActivity.apply {
            setActionBarTitle(
                "Payment",
                null
            )
            toggleActionBar(true)
            toggleBottomNavigation(false)
        }
    }

    fun handleLinkRequest(data: Uri): Boolean {
        try {
            val receiver_name = data.getQueryParameter("name")
            val receiver_public = data.getQueryParameter("public")
            val amount = data.getQueryParameter("amount")
            val message = data.getQueryParameter("message")
            val pkstring = data.getQueryParameter("key")
            val signature = data.getQueryParameter("signature")
            val host = data.getQueryParameter("host")
            val port = data.getQueryParameter("port")
            val paymentId = data.getQueryParameter("paymentId")
            val t2e = data.getQueryParameter("t2e")
            val pk = SecurityUtil.deserializePK(pkstring)
            var url = SecurityUtil.urldecode(data.toString())
            url = url.removeRange(0, url.indexOf("?") + 1)
            url = url.removeRange(url.indexOf("&signature"), url.length)
            if (amount != null && receiver_public != null && host != null && paymentId != null && pk != null && SecurityUtil.validate(url, signature, pk)) {
                setData(
                    receiver_name,
                    amount,
                    message,
                    receiver_public,
                    t2e,
                    host,
                    port,
                    paymentId
                )
                return true
            }
            return false
        } catch (ex: java.lang.Exception) {
            Log.e("handle_link", ex.message ?: ex.toString())
            return false
        }
    }

    fun setData(
        name: String?,
        amount: String,
        message: String?,
        public: String,
        e2t: String?,
        host: String,
        port: String?,
        paymentId: String
    ) {
        if (name != null) {
            this.ReceiverName = name
        }
        this.ReceiverPublic = public
        this.Amount = amount
        this.Message = message
        this.Host = host
        this.PaymentId = paymentId
        if (e2t != null && port != null) {
            this.E2T = e2t.toBoolean()
            this.Port = port
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        onResume()

        binding.pbPayingEuro.isVisible = false
        binding.tvPaymentReceiver.text = this.ReceiverName
        binding.tvPaymentAmount.text = this.Amount
        if (this.Message != null) {
            binding.llPaymentMessage.visibility = View.VISIBLE
            binding.tvPaymentMessage.text = this.Message ?: ""
        }

        if (!this.E2T) {
            binding.clPayEuro.visibility = View.VISIBLE
        }

        binding.clPayEuro.setOnClickListener {
            Log.d("server_payEuro", "${this.Host}, ${this.PaymentId}")
            binding.pbPayingEuro.isVisible = true
            openTikkieLink(this.Host, this.PaymentId)
        }

        binding.clPayEurotoken.setOnClickListener {
            if (!this.E2T) {
                @Suppress("DEPRECATION")
                Handler().postDelayed(
                    {
                        try {
                            // Create proposal block to the recipient
                            val publicKey =
                                defaultCryptoProvider.keyFromPublicBin(this.ReceiverPublic.hexToBytes())
                            val block = getTransactionRepository().sendTransferProposalSync(
                                publicKey.keyToBin(),
                                this.Amount.replace(",", "").toLong()
                            )
                            if (block == null) {
                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_insufficient_balance)
                                )
                            } else {
                                getPeerChatCommunity().sendMessageWithTransaction(
                                    this.Message ?: "",
                                    block.calculateHash(),
                                    publicKey,
                                    getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                                )

                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(
                                        R.string.snackbar_transfer_of,
                                        this.Amount,
                                        this.ReceiverName
                                    ),
                                    isShort = false
                                )

                                val previousFragment = parentFragmentManager.fragments.filter {
                                    it.tag == ValueTransferMainActivity.walletOverviewFragmentTag
                                }

                                parentFragmentManager.beginTransaction().apply {
                                    hide(this@ExchangeTransferMoneyLinkFragment)
                                    show(previousFragment[0])
                                }.commit()

                                (previousFragment[0] as VTFragment).initView()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            parentActivity.displayToast(
                                requireContext(),
                                resources.getString(R.string.snackbar_unexpected_error_occurred)
                            )
                        }
                    },
                    500
                )
            } else {
                try {
                    val publicKey = defaultCryptoProvider.keyFromPublicBin(this.ReceiverPublic.hexToBytes())
                    val block = getTransactionRepository().sendDestroyProposalWithPaymentID(publicKey.keyToBin(), this.Host, this.Port.toInt(), this.PaymentId, this.Amount.replace(",", "").toLong())

                    if (block == null) {
                        parentActivity.displayToast(
                            requireContext(),
                            resources.getString(R.string.snackbar_insufficient_balance)
                        )
                    } else {
                        getPeerChatCommunity().sendMessageWithTransaction(
                            this.Message ?: "",
                            block.calculateHash(),
                            publicKey,
                            getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                        )
                        parentActivity.displayToast(
                            requireContext(),
                            resources.getString(
                                R.string.snackbar_transfer_of,
                                this.Amount,
                                this.ReceiverName
                            ),
                            isShort = false
                        )

                        val previousFragment = parentFragmentManager.fragments.filter {
                            it.tag == ValueTransferMainActivity.walletOverviewFragmentTag
                        }

                        parentFragmentManager.beginTransaction().apply {
                            hide(this@ExchangeTransferMoneyLinkFragment)
                            show(previousFragment[0])
                        }.commit()

                        (previousFragment[0] as VTFragment).initView()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.snackbar_unexpected_error_occurred)
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onBackPressed(animated: Boolean = true) {
        val previousFragment = parentFragmentManager.fragments.filter {
            it.tag == ValueTransferMainActivity.walletOverviewFragmentTag
        }

        parentFragmentManager.beginTransaction().apply {
            if (animated) setCustomAnimations(0, R.anim.exit_to_right)
            hide(this@ExchangeTransferMoneyLinkFragment)
            if (animated) setCustomAnimations(R.anim.enter_from_left, 0)
            show(previousFragment[0])
        }.commit()

        (previousFragment[0] as VTFragment).initView()
    }

    fun openTikkieLink(host: String, paymetId: String) {
        val url = "$host/api/exchange/e2t/start_payment"
        val queue = Volley.newRequestQueue(this.parentActivity)
        // Post parameters
        val params = HashMap<String, String>()
        params["payment_id"] = paymetId
        val jsonObject = JSONObject(params as Map<*, *>)
        // Volley post request with parameters
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                Log.d("server_res_pay", response.toString())
                val gatewaydata = response.getJSONObject("payment_connection_data")
                Log.d("server_res_tikkie", gatewaydata.getString("url"))
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(gatewaydata.getString("url"))
                )
                binding.pbPayingEuro.isVisible = false
                startActivity(browserIntent)
            },
            { error ->
                Log.d("server_err_pay", error.message ?: error.toString())
                binding.pbPayingEuro.isVisible = false
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_unexpected_error_occurred)
                )
            }
        )
        // Add the volley post request to the request queue
        queue.add(request)
    }
}
