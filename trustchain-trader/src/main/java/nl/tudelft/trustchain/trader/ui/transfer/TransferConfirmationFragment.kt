package nl.tudelft.trustchain.trader.ui.transfer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_transfer_confirmation.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.trader.R

/**
 * Fragment for the success animation. Can handle some logic if necessary
 */
class TransferConfirmationFragment : BaseFragment(R.layout.fragment_transfer_confirmation) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        animation_view.run {
            animation_view.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    view.findNavController()
                        .navigate(TransferConfirmationFragmentDirections.actionTransferConfirmationFragmentToTransferFragment())
                }
            })
        }
    }
}
