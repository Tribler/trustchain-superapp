package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.text.SimpleDateFormat
import java.util.*

class IdentityDetailsDialog : VTDialogFragment() {

    private val dateOfBirthFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
    private lateinit var identity: Identity

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_details, null)

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            if (!getIdentityCommunity().hasIdentity()) {
                bottomSheetDialog.dismiss()
            }

            identity = getIdentityCommunity().getIdentity()!!

            // Dialog cannot be discarded on outside touch
            bottomSheetDialog.setCancelable(false)
            bottomSheetDialog.setCanceledOnTouchOutside(false)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            // Force the dialog to be undraggable
            bottomSheetDialog.behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })

            val buttonCancel = view.findViewById<Button>(R.id.btnCancel)

            buttonCancel.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            val editTexts = mapOf<String, EditText>(
                KEY_GIVEN_NAMES to view.findViewById(R.id.etGivenNames),
                KEY_SURNAME to view.findViewById(R.id.etSurname),
                KEY_DATE_OF_BIRTH to view.findViewById(R.id.etDateOfBirth),
                KEY_DATE_OF_EXPIRY to view.findViewById(R.id.etDateOfExpiry),
                KEY_NATIONALITY to view.findViewById(R.id.etNationality),
                KEY_PERSONAL_NUMBER to view.findViewById(R.id.etPersonalNumber),
                KEY_DOCUMENT_NUMBER to view.findViewById(R.id.etDocumentNumber)
            )

            val btnMale = view.findViewById<MaterialButton>(R.id.btnMale)
            val btnFemale = view.findViewById<MaterialButton>(R.id.btnFemale)
            var btnMaleChecked = false
            var btnFemaleChecked = false
            val selectedGenderColor = getColorIDFromThemeAttribute(parentActivity, R.attr.mutedColor)
            val notSelectedGenderColor = R.color.light_gray

            val saveButton = view.findViewById<Button>(R.id.btnSaveIdentity)
            saveButton.isEnabled = true

            editTexts.forEach { map ->
                map.value.doAfterTextChanged {
                    toggleButton(saveButton, validateEditTexts(editTexts) && (btnMaleChecked || btnFemaleChecked))
                }
            }

            editTexts[KEY_GIVEN_NAMES]!!.setText(identity.content.givenNames)
            editTexts[KEY_SURNAME]!!.setText(identity.content.surname)
            editTexts[KEY_DATE_OF_BIRTH]!!.setText(dateOfBirthFormat.format(identity.content.dateOfBirth))
            editTexts[KEY_DATE_OF_EXPIRY]!!.setText(dateOfBirthFormat.format(identity.content.dateOfBirth))
            editTexts[KEY_NATIONALITY]!!.setText(identity.content.nationality)
            editTexts[KEY_PERSONAL_NUMBER]!!.setText(identity.content.personalNumber.toString())
            editTexts[KEY_DOCUMENT_NUMBER]!!.setText(identity.content.documentNumber)

            when (identity.content.gender) {
                VALUE_MALE -> {
                    btnMaleChecked = true
                    btnFemaleChecked = false

                    btnMale.apply {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                selectedGenderColor
                            )
                        )
                        setTextColor(Color.WHITE)
                    }
                }
                VALUE_FEMALE -> {
                    btnMaleChecked = false
                    btnFemaleChecked = true

                    btnFemale.apply {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                selectedGenderColor
                            )
                        )
                        setTextColor(Color.WHITE)
                    }
                }
            }

            btnMale.setOnClickListener {
                if (!btnMaleChecked) {
                    btnMaleChecked = true
                    btnFemaleChecked = false
                    btnMale.apply {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                selectedGenderColor
                            )
                        )
                        setTextColor(Color.WHITE)
                    }
                    btnFemale.apply {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                notSelectedGenderColor
                            )
                        )
                        setTextColor(Color.BLACK)
                    }

                    toggleButton(saveButton, validateEditTexts(editTexts))
                }
            }

            btnFemale.setOnClickListener {
                if (!btnFemaleChecked) {
                    btnMaleChecked = false
                    btnFemaleChecked = true
                    btnFemale.apply {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                selectedGenderColor
                            )
                        )
                        setTextColor(Color.WHITE)
                    }
                    btnMale.apply {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                notSelectedGenderColor
                            )
                        )
                        setTextColor(Color.BLACK)
                    }

                    toggleButton(saveButton, validateEditTexts(editTexts))
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            saveButton.setOnClickListener {
                val givenNames = editTexts[KEY_GIVEN_NAMES]!!.text.toString()
                val surname = editTexts[KEY_SURNAME]!!.text.toString()
                val gender = when {
                    btnMaleChecked -> VALUE_MALE
                    btnFemaleChecked -> VALUE_FEMALE
                    else -> VALUE_NEUTRAL
                }

                identity.content.givenNames = givenNames
                identity.content.surname = surname
                identity.content.gender = gender

                try {
                    getIdentityStore().editIdentity(identity)
                    bottomSheetDialog.dismiss()
                    parentActivity.invalidateOptionsMenu()

                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.snackbar_identity_update_success)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.snackbar_identity_update_error)
                    )
                }
            }
            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    private fun validateEditTexts(map: Map<String, EditText>): Boolean {
        return map.none {
            it.value.text.isEmpty()
        }
    }

    companion object {
        const val KEY_GIVEN_NAMES = "givenNames"
        const val KEY_SURNAME = "surname"
        const val KEY_DATE_OF_BIRTH = "dateOfBirth"
        const val KEY_DATE_OF_EXPIRY = "dateOfExpiry"
        const val KEY_NATIONALITY = "nationality"
        const val KEY_PERSONAL_NUMBER = "personalNumber"
        const val KEY_DOCUMENT_NUMBER = "documentNumber"

        const val VALUE_MALE = "M"
        const val VALUE_FEMALE = "F"
        const val VALUE_NEUTRAL = "X"
    }
}
