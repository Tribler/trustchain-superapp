package com.example.musicdao.generator

import com.example.musicdao.ipv8.MusicCommunity
import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import com.mpatric.mp3agic.Mp3File
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import org.bitcoinj.core.ECKey
import java.io.File

private val lazySodium = LazySodiumJava(SodiumJava())

class DataFeeder {
    private fun createTrustChainStore(): TrustChainSQLiteStore {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val database = Database(driver)
        return TrustChainSQLiteStore(database)
    }

    fun feed(dir: File) {
        if (!dir.isDirectory) return

        val settings = TrustChainSettings()
        val store = createTrustChainStore()
        val community = MusicCommunity.Factory(settings = settings, database = store).create()

        val walletMap = mutableMapOf<String, String>() // Artist name, public Wallet Key
        val ipv8Map = mutableMapOf<String, String>() // Artist name, public IPv8 key

//        val btcParams = CryptoCurrencyConfig.networkParams

        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                for (audioFile in file.listFiles()) {
                    try {
                        val mp3File = Mp3File(audioFile)
                        // TODO assuming they all have id3v2 tags?
                        if (mp3File.hasId3v1Tag()) {
                            val artist = mp3File.id3v2Tag.albumArtist
                            val album = mp3File.id3v2Tag.album
                            val year = mp3File.id3v2Tag.year

                            if (!walletMap.containsKey(artist)) {
                                // Create wallet
                                val key = ECKey().publicKeyAsHex
                                walletMap[artist] = key
                            }
                            val bitcoinPublicKey = walletMap[artist] ?: continue
                            if (!ipv8Map.containsKey(artist)) {
                                // Create ipv8 id
                                val key = JavaCryptoProvider.generateKey()
                                ipv8Map[artist] = key.toString()
                            }
                            val privateKeyString = ipv8Map[artist] ?: continue
                            val privateKey = LibNaClSK.fromBin(
                                privateKeyString.hexToBytes(), lazySodium
                            )
                            community.myPeer = Peer(privateKey)
                            val publicKey = community.myPeer.publicKey
                            val transaction = mutableMapOf<String, String>(
                                "magnet" to "magnet", // TODO find the magnet link of torrent file
                                "title" to album,
                                "artists" to artist,
                                "date" to year,
                                "torrentInfoName" to "torrentInfoName", // TODO
                                "publisher" to bitcoinPublicKey
                            )
                            IPv8Android.getInstance().start()
                            // TODO we now have these blocks locally, but how do we publish them?
                            // OR, how do we impersonate different IPv8 addresses on a phone? (Is
                            // that even necessary)?
                            community.createProposalBlock(
                                "publish_release",
                                transaction,
                                publicKey.keyToBin()
                            )
                            // Check if exists in map
                            // Optional create ipv8 id
                            // Create trustchain block
                            // Publish transaction
                        } else if (mp3File.hasId3v1Tag()) {
//                            val artist = mp3File.id3v1Tag.artist // In this case we select the
//                            // artist from the first song, which is not optimal...
//                            val album = mp3File.id3v1Tag.album
//                            val year = mp3File.id3v1Tag.year
                        } else {
                            continue
                        }
                    } catch (e: Exception) {}
                }
            }
        }
    }
}
