package nl.tudelft.trustchain.eurotoken.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.media.MediaPlayer
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity

open class EurotokenBaseFragment(contentLayoutId: Int = 0) : BaseFragment(contentLayoutId) {

    protected val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, gatewayStore)
    }

    private val contactStore by lazy {
        ContactStore.getInstance(requireContext())
    }

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val onReceiveListener = object : BlockListener {
        override fun onBlockReceived(block: TrustChainBlock) {
            if (block.isAgreement && block.publicKey.contentEquals(transactionRepository.trustChainCommunity.myPeer.publicKey.keyToBin())) {
                playMoneySound()
                makeMoneyToast()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        lifecycleScope.launchWhenResumed {
        }
    }

    fun makeMoneyToast(){
        Toast.makeText(requireContext(), "Money received!", Toast.LENGTH_LONG).show()
    }

    fun playMoneySound() {
        val afd = activity?.assets?.openFd("Coins.mp3") ?: return
        val player = MediaPlayer();
        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length);
        player.prepare();
        player.start();
    }


    override fun onResume() {
        transactionRepository.trustChainCommunity.addListener(TransactionRepository.BLOCK_TYPE_TRANSFER, onReceiveListener)
        transactionRepository.trustChainCommunity.addListener(TransactionRepository.BLOCK_TYPE_CREATE, onReceiveListener)
        super.onResume()
    }

    override fun onPause() {
        transactionRepository.trustChainCommunity.removeListener(onReceiveListener, TransactionRepository.BLOCK_TYPE_TRANSFER)
        transactionRepository.trustChainCommunity.removeListener(onReceiveListener, TransactionRepository.BLOCK_TYPE_CREATE)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.eurotoken_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val myPublicKey = getIpv8().myPeer.publicKey.keyToBin().toHex()
        return when (item.itemId) {
            R.id.verifyBalance -> {
                val gateway = transactionRepository.getGatewayPeer()
                if (gateway == null) {
                    Toast.makeText(requireContext(), "No preferred gateway set", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    transactionRepository.sendCheckpointProposal(gateway)
                    Toast.makeText(requireContext(), "CHECKPOINT", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.copyKey -> {
                val clipboard =
                    ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
                val clip = ClipData.newPlainText("Public Key", myPublicKey)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.renameSelf -> {
                renameSelf()
                true
            }
            R.id.gateways -> {
                findNavController().navigate(R.id.gatewaysFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun renameSelf() {
        val myKey = getIpv8().myPeer.publicKey

        val contact = contactStore.getContactFromPublicKey(myKey)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Rename Contact")

        // Set up the input
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(contact?.name ?: "")
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton(
            "Rename"
        ) { _, _ ->
            val ans = input.text.toString()
            if (ans == "") {
                if (contact != null) {
                    contactStore.deleteContact(contact)
                }
            } else {
                contactStore.updateContact(myKey, input.text.toString())
            }
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }


}
