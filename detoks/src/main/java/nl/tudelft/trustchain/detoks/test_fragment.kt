package nl.tudelft.trustchain.detoks

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.android.synthetic.main.test_fragment_layout.*
import nl.tudelft.ipv8.sqldelight.Database
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.android.synthetic.main.discovered_peer_item.*
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment

private const val PREF_PRIVATE_KEY = "private_key"
private const val PREF_PUBLIC_KEY = "public_key"

class test_fragment : BaseFragment(R.layout.test_fragment_layout), singleTransactionOnClick, confirmProposalOnClick {

    var peers: ArrayList<PeerViewModel> = arrayListOf()
    var proposals: ArrayList<ProposalViewModel> = arrayListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getPrivateKey(requireContext())


//        val luukCommunity = OverlayConfiguration(
//            Overlay.Factory(LuukCommunity::class.java),
//            listOf(RandomWalk.Factory())
//        )

//        val configuration = IPv8Configuration(overlays=listOf(luukCommunity))

//        System.out.println("Application returns " + Application())
//        System.out.println("Context is " + context.toString())

//        activity?.let { IPv8Android.Factory(it.application).setConfiguration(configuration).setPrivateKey(getPrivateKey(requireContext())).init() }
        // wow new comment

        //Do the trustchain things
        val settings = TrustChainSettings()
        val randomWalk = RandomWalk.Factory()
        // Initialize storage
        val driver = AndroidSqliteDriver(Database.Schema, requireContext(), "trustchain.db")
        val store = TrustChainSQLiteStore(Database(driver))

        // Create the community
        val luuksTrustChainCommunity = OverlayConfiguration(
            TrustChainCommunity.Factory(settings, store),
            listOf(randomWalk)
        )

        // We now initialize IPv8 with this new community
        val trustChainConfiguration = IPv8Configuration(overlays=listOf(luuksTrustChainCommunity))
        activity?.let { IPv8Android.Factory(it.application).setConfiguration(trustChainConfiguration).setPrivateKey(getPrivateKey(requireContext())).init() }

        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!

        // Call this method to register the validator.
        registerValidator(trustchain)

        // Register the BlockSigner
        //registerSigner(trustchain)

        val peerRecyclerView = peerListView
        peerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = PeerAdapter(peers, this)

        peerRecyclerView.adapter = adapter

        val proposalRecyclerView = proposalsListView
        proposalRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val proposalAdapter = ProposalAdapter(proposals, this)
        proposalRecyclerView.adapter = proposalAdapter


        Thread(Runnable {

            val trustChainCommunity = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()
            if (trustChainCommunity != null) {
                while (true) {
                    println("alert: getting peers, found ${Integer.toString(peers.size)}")
                    val peerlist :List<Peer> = trustChainCommunity.getPeers()

                    requireActivity().runOnUiThread(Runnable {
                        for (peer in peerlist) {
                            if (!peers.contains(PeerViewModel(peer.publicKey.toString(), peer))) {
                                peers.add(PeerViewModel(peer.publicKey.toString(), peer))
                                adapter.notifyItemInserted(peers.size-1)
                            }
                        }

                    })
                    Thread.sleep(100)
                }


            }

        }).start()


        trustchain.addListener("test_block", object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                println("we received a block from ${block.publicKey}, amazing")
                // If we receive a proposal of the correct type...

                if (block.isProposal) {
                    proposals.add(ProposalViewModel(block.publicKey.toString(), block.type, block))
                    proposalAdapter.notifyItemInserted(proposals.size - 1)
                }
            }
        })

    }






    // We also register a TransactionValidator. This one is simple and checks whether "test_block"s are valid.
    private fun registerValidator(trustchain : TrustChainCommunity) {
        trustchain.registerTransactionValidator("test_block", object : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): ValidationResult {
                if (block.transaction["message"] == "luuk_test" || block.isAgreement || block.isProposal) {
                    return ValidationResult.Valid
                } else {
                    if (block.transaction["message"] != "luuk_test") {
                        return ValidationResult.Invalid(listOf("The message weas not equal to luuk_test"))
                    } else {
                        return ValidationResult.Invalid(listOf("This block was not an agreement"))
                    }
                }
            }
        })
    }

    // This function registers a BlockSigner. We will notify it of incoming valid blocks, and this method will make sure an agreement is generated for it.
    // Again, this will only register the signer for blocks of type test_block.
    // Note that this signer will be called at all incoming proposals, so it will automatically reply. This will of course be necessary later, but for now it might be nice
    // to reply to incoming proposals manually.
    private fun registerSigner(trustchain : TrustChainCommunity) {
        trustchain.registerBlockSigner("test_block", object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                trustchain.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
    }




    // Create a proposal block, store it, and send it to all peers. It sends blocks of type "test_block"
    private fun createProposal(recipient : Peer) {
        // get reference to the trustchain community
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!

        val transaction = mapOf("message" to "test message!")
        trustchain.createProposalBlock("test_block", transaction, recipient.publicKey.keyToBin())
    }

    private fun getPrivateKey(context: Context): nl.tudelft.ipv8.keyvault.PrivateKey {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val privateKey = prefs.getString(PREF_PRIVATE_KEY, null)
        if (privateKey == null) {
            val newKey = AndroidCryptoProvider.generateKey()
            prefs.edit().putString(PREF_PRIVATE_KEY, newKey.keyToBin().toHex()).apply();
            privateKeyStatus.setText("There was no key stored yet. ")
            privateKeyValue.setText(newKey.toString())
            return newKey
        } else {
            privateKeyStatus.setText("A key was already stored.\n Public key: ${AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes()).pub()}")
            privateKeyValue.setText(privateKey)
            return AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes())
        }
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
        println("Luuk community: ${peer.mid}: ${payload.message}")
    }

    class MyMessage(val message: String) : Serializable {
        override fun serialize(): ByteArray {
            return message.toByteArray()
        }

        companion object Deserializer : Deserializable<MyMessage> {
            override fun deserialize(buffer: ByteArray, offset: Int): Pair<MyMessage, Int> {
                return Pair(MyMessage(buffer.toString(Charsets.UTF_8)), buffer.size)
            }
        }
    }

    override fun onClick(recipient: Peer) {
        println("alert: onClick called from the adapter!")
        createProposal(recipient)
    }

    override fun confirmProposalClick(block: TrustChainBlock) {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        trustchain.createAgreementBlock(block, mapOf<Any?, Any?>())
        println("alert: Agreement should have been sent!")
    }


}

public interface singleTransactionOnClick {
    fun onClick(recipient: Peer)
}

interface confirmProposalOnClick {
    fun confirmProposalClick(block: TrustChainBlock)
}
