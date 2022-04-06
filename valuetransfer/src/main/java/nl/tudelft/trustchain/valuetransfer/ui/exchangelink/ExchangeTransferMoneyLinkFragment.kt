package nl.tudelft.trustchain.valuetransfer.ui.exchangelink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.lifecycle.*
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentExchangeTransferMoneyLinkBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.*
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import org.json.JSONObject

class ExchangeTransferMoneyLinkFragment : VTFragment(R.layout.fragment_exchange_transfer_money_link) {
    private val binding by viewBinding(FragmentExchangeTransferMoneyLinkBinding::bind)

    private var ReceiverName=""
    private var ReceiverPublic=""
    private var Amount=""
    private var Message: String? = null
    private var IBAN: String? = null
    private var Host=""
    private var PaymentId=""

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

    fun setData(name: String?, amount: String, message: String?, public: String,
                iban: String?, host: String, paymentId: String)
    {
        if (name != null) {
            this.ReceiverName = name
        }
        this.ReceiverPublic = public
        this.Amount = amount
        this.Message = message
        this.IBAN = iban
        this.Host = host
        this.PaymentId = paymentId
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
            binding.tvPaymentMessage.text = this.Message?:""
        }

        if (this.IBAN == null) {
            binding.clPayEuro.visibility = View.VISIBLE
        }

        binding.clPayEuro.setOnClickListener {
            Log.d("server_payEuro", "${this.Host}, ${this.PaymentId}")
            binding.pbPayingEuro.isVisible = true
            openTikkieLink(this.Host, this.PaymentId)
        }

        binding.clPayEurotoken.setOnClickListener {
            if (this.IBAN == null) {
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
                    val block = getTransactionRepository().sendDestroyProposalWithIBAN(this.IBAN!!, this.Amount.replace(",", "").toLong())

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

    fun openTikkieLink(host: String, paymetId: String)
    {
        val url = "$host/api/exchange/e2t/start_payment"
        val queue = Volley.newRequestQueue(this.parentActivity)
        // Post parameters
        val params = HashMap<String,String>()
        params["payment_id"] = paymetId
        val jsonObject = JSONObject(params as Map<*, *>)
        // Volley post request with parameters
        val request = JsonObjectRequest(
            Request.Method.POST,url,jsonObject,
            { response ->
                Log.d("server_res_pay", response.toString())
                val gatewaydata = response.getJSONObject("payment_connection_data")
                Log.d("server_res_tikkie", gatewaydata.getString("url"))
                val browserIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse(gatewaydata.getString("url")))
                binding.pbPayingEuro.isVisible = false
                startActivity(browserIntent)
            }, { error ->
                Log.d("server_err_pay", error.message?: error.toString())
                binding.pbPayingEuro.isVisible = false
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_unexpected_error_occurred)
                )
            })
        // Add the volley post request to the request queue
        queue.add(request);
    }
}
