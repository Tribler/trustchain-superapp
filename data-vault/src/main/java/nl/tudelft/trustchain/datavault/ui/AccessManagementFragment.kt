package nl.tudelft.trustchain.datavault.ui

import android.os.Bundle
import android.view.View
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.accesscontrol.AccessPolicy
import nl.tudelft.trustchain.datavault.databinding.AccessManagementFragmentBinding
import nl.tudelft.trustchain.datavault.databinding.VaultBrowserFragmentBinding

class AccessManagementFragment :BaseFragment(R.layout.access_management_fragment) {
    private val binding by viewBinding(AccessManagementFragmentBinding::bind)

    private lateinit var accessPolicy: AccessPolicy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val fileName = savedInstanceState
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //binding.fileNameTextView
    }
}
