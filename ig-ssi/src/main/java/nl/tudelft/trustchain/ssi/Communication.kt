package nl.tudelft.trustchain.ssi

import nl.tudelft.ipv8.attestation.communication.CommunicationChannel // OK
import nl.tudelft.ipv8.attestation.communication.CommunicationManager // OK

const val DEFAULT_PSEUDONYM = "MY_PEER"
val DEFAULT_RENDEZVOUS_TOKEN = null

object Communication {

    private var communicationManager: CommunicationManager? = null
    private var activePseudonym: String? = null
    private var activeRendezvousToken: String? = null

    val pseudonymLock = Object()
    val tokenLock = Object()

    fun getInstance(): CommunicationManager {
        return communicationManager
            ?: throw IllegalStateException("CommunicationManager is not initialized.")
    }

    fun load(
        pseudonym: String = getActivePseudonym(),
        rendezvous: String? = getActiveRendezvousToken()
    ): CommunicationChannel {
        var rendezvousToken = rendezvous
        if (rendezvousToken == "") {
            rendezvousToken = DEFAULT_RENDEZVOUS_TOKEN
        }
        if (rendezvousToken != activeRendezvousToken) {
            setActiveRendezvousToken(rendezvousToken)
        }
        return getInstance().load(pseudonym, rendezvousToken)
    }

    fun getActivePseudonym(): String {
        return activePseudonym
            ?: throw java.lang.IllegalStateException("ActivePseudonym is not initialized.")
    }

    fun getActiveRendezvousToken(): String? {
        return activeRendezvousToken
    }

    fun setActivePseudonym(pseudonym: String) {
        synchronized(pseudonymLock) {
            this.activePseudonym = pseudonym
        }
    }

    fun setActiveRendezvousToken(token: String?) {
        if (activeRendezvousToken != token)
        synchronized(tokenLock) {
            val manager = getInstance()
            manager.unload(getActivePseudonym())
            this.activeRendezvousToken = token
            this.load()
        }
    }

    class Factory(communicationManager: CommunicationManager) {
        init {
            Communication.communicationManager = communicationManager
            activePseudonym = DEFAULT_PSEUDONYM
            communicationManager.load(getActivePseudonym())
        }
    }
}
