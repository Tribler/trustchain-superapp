package com.example.musicdao.wallet

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.musicdao.MusicService
import com.example.musicdao.R
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


class WalletFragment: Fragment(R.layout.fragment_wallet) {
    lateinit var musicWallet: MusicWallet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)

        musicWallet = (activity as MusicService).musicWallet
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val progress = musicWallet.percentageSynced
                if (progress < 100) {
                    blockchain_progress.progress = progress
                    wallet_status.text = "Syncing chain... progress: ${progress}%"
                }
                if (progress == 100) {
                    blockchain_progress.progress = 100
                    wallet_public_key.text = musicWallet.publicKey()
                    wallet_balance.text = musicWallet.balanceText()
                    wallet_status.text = "Finished syncing chain"
                }

                wallet_status_2.text = musicWallet.status()
                delay(1000)
            }
        }
    }
}
