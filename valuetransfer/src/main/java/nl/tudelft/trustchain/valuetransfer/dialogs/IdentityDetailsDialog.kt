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
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.text.SimpleDateFormat
import java.util.*

class IdentityDetailsDialog : DialogFragment() {

    private var cal = Calendar.getInstance()
    private val dateOfBirthFormat = SimpleDateFormat("MMMM d, yyyy")

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var identityCommunity: IdentityCommunity
    private lateinit var identityStore: IdentityStore
    private var identity: Identity? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_details, null)

            parentActivity = requireActivity() as ValueTransferMainActivity
            identityCommunity = parentActivity.getCommunity()!!
            identityStore = parentActivity.getStore()!!

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            identity = identityStore.getIdentity()

            // Dialog cannot be discarded on outside touch
            bottomSheetDialog.setCancelable(false)
            bottomSheetDialog.setCanceledOnTouchOutside(false)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            // Force the dialog to be undraggable
            bottomSheetDialog.behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })

            if (!identityStore.hasIdentity()) {
                bottomSheetDialog.setOnKeyListener { _, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        parentActivity.displaySnackbar(requireContext(), "Adding identity cancelled. Without an identity you'll not be able to use this application.", type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING, isShort = false)
                        bottomSheetDialog.dismiss()
                        return@setOnKeyListener true
                    }
                    false
                }
            }

            view.findViewById<ConstraintLayout>(R.id.clCancel).isVisible = identityStore.hasIdentity()
            val buttonCancel = view.findViewById<Button>(R.id.btnCancel)

            buttonCancel.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            val editTexts = mapOf<String, EditText>(
                "givenNames" to view.findViewById(R.id.etGivenNames),
                "surname" to view.findViewById(R.id.etSurname),
                "placeOfBirth" to view.findViewById(R.id.etPlaceOfBirth),
                "dateOfBirth" to view.findViewById(R.id.etDateOfBirth),
                "nationality" to view.findViewById(R.id.etNationality),
                "personalNumber" to view.findViewById(R.id.etPersonalNumber),
                "documentNumber" to view.findViewById(R.id.etDocumentNumber)
            )

            val btnMale = view.findViewById<MaterialButton>(R.id.btnMale)
            val btnFemale = view.findViewById<MaterialButton>(R.id.btnFemale)
            var btnMaleChecked = false
            var btnFemaleChecked = false

            val saveButton = view.findViewById<Button>(R.id.btnSaveIdentity)
            toggleButton(saveButton, identity != null)

            editTexts.forEach { map ->
                map.value.doAfterTextChanged {
                    toggleButton(saveButton, validateEditTexts(editTexts) && (btnMaleChecked || btnFemaleChecked))
                }
            }

            if (identity != null) {
                editTexts["givenNames"]!!.setText(identity!!.content.givenNames)
                editTexts["surname"]!!.setText(identity!!.content.surname)
                editTexts["placeOfBirth"]!!.setText(identity!!.content.placeOfBirth)
                editTexts["dateOfBirth"]!!.setText(SimpleDateFormat("MMMM d, yyyy").format(identity!!.content.dateOfBirth))

                val day = SimpleDateFormat("d").format(identity!!.content.dateOfBirth).toInt()
                val month = SimpleDateFormat("M").format(identity!!.content.dateOfBirth).toInt()
                val year = SimpleDateFormat("yyyy").format(identity!!.content.dateOfBirth).toInt()
                cal.set(year, month - 1, day)

                editTexts["nationality"]!!.setText(identity!!.content.nationality)
                editTexts["personalNumber"]!!.setText(identity!!.content.personalNumber.toString())
                editTexts["documentNumber"]!!.setText(identity!!.content.documentNumber)

                when (identity!!.content.gender) {
                    "Male" -> {
                        btnMaleChecked = true
                        btnFemaleChecked = false

                        btnMale.setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryDarkValueTransfer
                            )
                        )
                        btnMale.setTextColor(Color.WHITE)
                    }
                    "Female" -> {
                        btnMaleChecked = false
                        btnFemaleChecked = true

                        btnFemale.setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryDarkValueTransfer
                            )
                        )
                        btnFemale.setTextColor(Color.WHITE)
                    }
                }
            }

            btnMale.setOnClickListener {
                if (!btnMaleChecked) {
                    btnMaleChecked = true
                    btnFemaleChecked = false
                    btnMale.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorPrimaryDarkValueTransfer
                        )
                    )
                    btnMale.setTextColor(Color.WHITE)
                    btnFemale.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.light_gray
                        )
                    )
                    btnFemale.setTextColor(Color.BLACK)

                    toggleButton(saveButton, validateEditTexts(editTexts))
                }
            }

            btnFemale.setOnClickListener {
                if (!btnFemaleChecked) {
                    btnMaleChecked = false
                    btnFemaleChecked = true
                    btnFemale.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorPrimaryDarkValueTransfer
                        )
                    )
                    btnFemale.setTextColor(Color.WHITE)
                    btnMale.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.light_gray
                        )
                    )
                    btnMale.setTextColor(Color.BLACK)

                    toggleButton(saveButton, validateEditTexts(editTexts))
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            val dateSetListener = object : DatePickerDialog.OnDateSetListener {
                override fun onDateSet(
                    view: DatePicker,
                    year: Int,
                    monthOfYear: Int,
                    dayOfMonth: Int
                ) {
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, monthOfYear)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    val date = dateOfBirthFormat.format(cal.time)
                    editTexts["dateOfBirth"]!!.setText(date)
                }
            }

            editTexts["dateOfBirth"]!!.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    DatePickerDialog(
                        requireContext(), R.style.DatePickerDialogTheme, dateSetListener, cal.get(Calendar.YEAR),
                        cal.get(
                            Calendar.MONTH
                        ),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            })

            saveButton.setOnClickListener {
                val givenNames = editTexts["givenNames"]!!.text.toString()
                val surname = editTexts["surname"]!!.text.toString()
                val placeOfBirth = editTexts["placeOfBirth"]!!.text.toString()
                val dateOfBirth = (dateOfBirthFormat.parse(editTexts["dateOfBirth"]!!.text.toString()) as Date).time
                val nationality = editTexts["nationality"]!!.text.toString()
                val gender = when {
                    btnMaleChecked -> "Male"
                    btnFemaleChecked -> "Female"
                    else -> "Neutral"
                }
                val personalNumber = editTexts["personalNumber"]!!.text.toString().toLong()
                val documentNumber = editTexts["documentNumber"]!!.text.toString()

                if (identity == null) {
                    try {
                        val newIdentity = identityCommunity.createIdentity(givenNames, surname, placeOfBirth, dateOfBirth, nationality, gender, personalNumber, documentNumber)
                        identityStore.addIdentity(newIdentity)
                        bottomSheetDialog.dismiss()
                        parentActivity.displaySnackbar(requireContext(), "Identity successfully added. Application re-initialized.")
                        parentActivity.reloadActivity()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        parentActivity.displaySnackbar(requireContext(), "Identity couldn't be added", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
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
                        identityStore.editIdentity(identity!!)
                        bottomSheetDialog.dismiss()
                        parentActivity.invalidateOptionsMenu()

                        parentActivity.displaySnackbar(requireContext(), "Identity successfully updated")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        parentActivity.displaySnackbar(requireContext(), "Identity couldn't be updated", view = parentActivity.getView(true), type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                    }
                }
            }
            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun validateEditTexts(map: Map<String, EditText>): Boolean {
        return map.none {
            it.value.text.isEmpty()
        }
    }
}
