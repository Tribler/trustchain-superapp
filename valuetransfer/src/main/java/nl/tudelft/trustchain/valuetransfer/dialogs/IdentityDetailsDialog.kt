package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.text.SimpleDateFormat
import java.util.*

class IdentityDetailsDialog : VTDialogFragment() {

    private var cal = Calendar.getInstance()
    private val dateOfBirthFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
    private val dayFormat = SimpleDateFormat("d", Locale.ENGLISH)
    private val monthFormat = SimpleDateFormat("M", Locale.ENGLISH)
    private val yearFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)
    private var identity: Identity? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_details, null)

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            identity = getIdentityCommunity().getIdentity()

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

            if (!getIdentityCommunity().hasIdentity()) {
                bottomSheetDialog.setOnKeyListener { _, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_identity_add_cancelled),
                            type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING,
                            isShort = false
                        )
                        bottomSheetDialog.dismiss()
                        return@setOnKeyListener true
                    }
                    false
                }
            }

            view.findViewById<ConstraintLayout>(R.id.clCancel).isVisible = getIdentityCommunity().hasIdentity()
            val buttonCancel = view.findViewById<Button>(R.id.btnCancel)

            buttonCancel.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            val editTexts = mapOf<String, EditText>(
                KEY_GIVEN_NAMES to view.findViewById(R.id.etGivenNames),
                KEY_SURNAME to view.findViewById(R.id.etSurname),
                KEY_PLACE_OF_BIRTH to view.findViewById(R.id.etPlaceOfBirth),
                KEY_DATE_OF_BIRTH to view.findViewById(R.id.etDateOfBirth),
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
            toggleButton(saveButton, identity != null)

            editTexts.forEach { map ->
                map.value.doAfterTextChanged {
                    toggleButton(saveButton, validateEditTexts(editTexts) && (btnMaleChecked || btnFemaleChecked))
                }
            }

            if (identity != null) {
                editTexts[KEY_GIVEN_NAMES]!!.setText(identity!!.content.givenNames)
                editTexts[KEY_SURNAME]!!.setText(identity!!.content.surname)
                editTexts[KEY_PLACE_OF_BIRTH]!!.setText(identity!!.content.placeOfBirth)
                editTexts[KEY_DATE_OF_BIRTH]!!.setText(dateOfBirthFormat.format(identity!!.content.dateOfBirth))

                val day = dayFormat.format(identity!!.content.dateOfBirth).toInt()
                val month = monthFormat.format(identity!!.content.dateOfBirth).toInt()
                val year = yearFormat.format(identity!!.content.dateOfBirth).toInt()
                cal.set(year, month - 1, day)

                editTexts[KEY_NATIONALITY]!!.setText(identity!!.content.nationality)
                editTexts[KEY_PERSONAL_NUMBER]!!.setText(identity!!.content.personalNumber.toString())
                editTexts[KEY_DOCUMENT_NUMBER]!!.setText(identity!!.content.documentNumber)

                when (identity!!.content.gender) {
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

            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthOfYear)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }

                dateOfBirthFormat.format(cal.time).let { dateString ->
                    editTexts[KEY_DATE_OF_BIRTH]!!.setText(dateString)
                }
            }

            editTexts[KEY_DATE_OF_BIRTH]!!.setOnClickListener {
                DatePickerDialog(
                    requireContext(),
                    R.style.DatePickerDialogTheme,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            saveButton.setOnClickListener {
                val givenNames = editTexts[KEY_GIVEN_NAMES]!!.text.toString()
                val surname = editTexts[KEY_SURNAME]!!.text.toString()
                val placeOfBirth = editTexts[KEY_PLACE_OF_BIRTH]!!.text.toString()
                val dateOfBirth = (dateOfBirthFormat.parse(editTexts[KEY_DATE_OF_BIRTH]!!.text.toString()) as Date).time
                val nationality = editTexts[KEY_NATIONALITY]!!.text.toString()
                val gender = when {
                    btnMaleChecked -> VALUE_MALE
                    btnFemaleChecked -> VALUE_FEMALE
                    else -> VALUE_NEUTRAL
                }
                val personalNumber = editTexts[KEY_PERSONAL_NUMBER]!!.text.toString().toLong()
                val documentNumber = editTexts[KEY_DOCUMENT_NUMBER]!!.text.toString()

                if (identity == null) {
                    try {
                        getIdentityCommunity().createIdentity(
                            givenNames,
                            surname,
                            placeOfBirth,
                            dateOfBirth,
                            nationality,
                            gender,
                            personalNumber,
                            documentNumber
                        ).let { newIdentity ->
                            getIdentityStore().addIdentity(newIdentity)
                        }

                        bottomSheetDialog.dismiss()
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_identity_add_success)
                        )
                        parentActivity.reloadActivity()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_identity_add_error),
                            type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                        )
                    }
                } else {
                    identity!!.content.givenNames = givenNames
                    identity!!.content.surname = surname
                    identity!!.content.placeOfBirth = placeOfBirth
                    identity!!.content.dateOfBirth = Date(dateOfBirth)
                    identity!!.content.nationality = nationality
                    identity!!.content.gender = gender
                    identity!!.content.personalNumber = personalNumber
                    identity!!.content.documentNumber = documentNumber

                    try {
                        getIdentityStore().editIdentity(identity!!)
                        bottomSheetDialog.dismiss()
                        parentActivity.invalidateOptionsMenu()

                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_identity_update_success)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_identity_update_error),
                            view = view.rootView,
                            type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                        )
                    }
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
        const val KEY_PLACE_OF_BIRTH = "placeOfBirth"
        const val KEY_DATE_OF_BIRTH = "dateOfBirth"
        const val KEY_NATIONALITY = "nationality"
        const val KEY_PERSONAL_NUMBER = "personalNumber"
        const val KEY_DOCUMENT_NUMBER = "documentNumber"

        const val VALUE_MALE = "Male"
        const val VALUE_FEMALE = "Female"
        const val VALUE_NEUTRAL = "Neutral"
    }
}
