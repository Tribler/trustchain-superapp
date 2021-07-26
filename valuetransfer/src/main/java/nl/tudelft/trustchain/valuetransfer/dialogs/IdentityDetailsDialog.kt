package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

class IdentityDetailsDialog(
    private val identity: Identity?,
    private val community: IdentityCommunity,
) : DialogFragment() {

    private var cal = Calendar.getInstance()

    private val identityStore by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private fun validateEditTexts(map: Map<String, EditText>): Boolean {
        return map.none {
            it.value.text.isEmpty()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_add, null)

            val editTexts = mapOf<String, EditText>(
                "givenNames" to view.findViewById(R.id.etGivenNames),
                "surname" to view.findViewById(R.id.etSurname),
                "placeOfBirth" to view.findViewById(R.id.etPlaceOfBirth),
                "dateOfBirth" to view.findViewById(R.id.etDateOfBirth),
                "nationality" to view.findViewById(R.id.etNationality),
                "personalNumber" to view.findViewById(R.id.etPersonalNumber),
                "documentNumber" to view.findViewById(R.id.etDocumentNumber)
            )

            val genderButtonGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.btnGenderGroup)
            val btnMale = view.findViewById<MaterialButton>(R.id.btnMale)
            val btnFemale = view.findViewById<MaterialButton>(R.id.btnFemale)

            val saveButton = view.findViewById<Button>(R.id.btnSaveIdentity)
            toggleButton(saveButton, identity != null)

            editTexts.forEach { map ->
                map.value.doAfterTextChanged {
                    toggleButton(saveButton, validateEditTexts(editTexts) && (btnMale.isChecked || btnFemale.isChecked))
                }
            }

            if (identity != null) {
                editTexts["givenNames"]!!.setText(identity.content.givenNames)
                editTexts["surname"]!!.setText(identity.content.surname)
                editTexts["placeOfBirth"]!!.setText(identity.content.placeOfBirth)
                editTexts["dateOfBirth"]!!.setText(SimpleDateFormat("MMMM d, yyyy").format(identity.content.dateOfBirth))

                val day = SimpleDateFormat("d").format(identity.content.dateOfBirth).toInt()
                val month = SimpleDateFormat("M").format(identity.content.dateOfBirth).toInt()
                val year = SimpleDateFormat("yyyy").format(identity.content.dateOfBirth).toInt()
                cal.set(year, month - 1, day)

                editTexts["nationality"]!!.setText(identity.content.nationality)
                editTexts["personalNumber"]!!.setText(identity.content.personalNumber.toString())
                editTexts["documentNumber"]!!.setText(identity.content.documentNumber)

                when(identity.content.gender) {
                    "Male" -> {
                        btnMale.isChecked = true
                        btnMale.isCheckable = false
                        btnMale.setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryDarkValueTransfer
                            )
                        )
                        btnMale.setTextColor(Color.WHITE)
                    }
                    "Female" -> {
                        btnFemale.isChecked = true
                        btnFemale.isCheckable = false
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

            genderButtonGroup.addOnButtonCheckedListener(MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, _ ->
//                if (isChecked) {
                    when(checkedId) {
                        R.id.btnMale -> {
                            btnMale.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.colorPrimaryDarkValueTransfer
                                )
                            )
                            btnMale.setTextColor(Color.WHITE)
                            btnFemale.setBackgroundColor(Color.WHITE)
                            btnFemale.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.colorPrimaryValueTransfer
                                )
                            )
                            btnMale.isCheckable = false
                            btnFemale.isCheckable = true
                            toggleButton(saveButton, validateEditTexts(editTexts))
                            Log.d("TESTJE", "MALE CLICKED")
                        }
                        R.id.btnFemale -> {
                            btnFemale.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.colorPrimaryDarkValueTransfer
                                )
                            )
                            btnFemale.setTextColor(Color.WHITE)
                            btnMale.setBackgroundColor(Color.WHITE)
                            btnMale.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.colorPrimaryValueTransfer
                                )
                            )
                            btnFemale.isCheckable = false
                            btnMale.isCheckable = true
                            toggleButton(saveButton, validateEditTexts(editTexts))
                            Log.d("TESTJE", "FEMALE CLICKED")
                        }
//                    }
//                    if (checkedId == R.id.btnMale && !view.findViewById<MaterialButton>(R.id.btnMale).isChecked) {
//                        view.findViewById<Button>(R.id.btnMale).setBackgroundColor(
//                            ContextCompat.getColor(
//                                requireContext(),
//                                R.color.colorPrimaryDarkValueTransfer
//                            )
//                        )
//                        view.findViewById<Button>(R.id.btnMale).setTextColor(Color.WHITE)
//                        view.findViewById<Button>(R.id.btnFemale).setBackgroundColor(Color.WHITE)
//                        view.findViewById<Button>(R.id.btnFemale).setTextColor(
//                            ContextCompat.getColor(
//                                requireContext(),
//                                R.color.colorPrimaryValueTransfer
//                            )
//                        )
//                        genderChecked = true
//                        toggleButton(saveButton, validateEditTexts(editTexts))
//                    } else if(checkedId == R.id.btnFemale && !view.findViewById<MaterialButton>(R.id.btnFemale).isChecked) {
//                        view.findViewById<Button>(R.id.btnFemale).setBackgroundColor(
//                            ContextCompat.getColor(
//                                requireContext(),
//                                R.color.colorPrimaryDarkValueTransfer
//                            )
//                        )
//                        view.findViewById<Button>(R.id.btnFemale).setTextColor(Color.WHITE)
//                        view.findViewById<Button>(R.id.btnMale).setBackgroundColor(Color.WHITE)
//                        view.findViewById<Button>(R.id.btnMale).setTextColor(
//                            ContextCompat.getColor(
//                                requireContext(),
//                                R.color.colorPrimaryValueTransfer
//                            )
//                        )
//                        genderChecked = true
//                        toggleButton(saveButton, validateEditTexts(editTexts))
//                    }
                }
            })

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            val dateSetListener = object : DatePickerDialog.OnDateSetListener {
                override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, monthOfYear)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    val date = SimpleDateFormat("MMMM d, yyyy").format(cal.time)
                    editTexts["dateOfBirth"]!!.setText(date)
                }
            }

            editTexts["dateOfBirth"]!!.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    DatePickerDialog(requireContext(), R.style.DatePickerDialogTheme, dateSetListener, cal.get(Calendar.YEAR), cal.get(
                        Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }
            })

            saveButton.setOnClickListener {
                val givenNames = editTexts["givenNames"]!!.text.toString()
                val surname = editTexts["surname"]!!.text.toString()
                val placeOfBirth = editTexts["placeOfBirth"]!!.text.toString()
                val dateOfBirth = (SimpleDateFormat("MMMM d, yyyy").parse(editTexts["dateOfBirth"]!!.text.toString()) as Date).time
                val nationality = editTexts["nationality"]!!.text.toString()
                val gender = when(genderButtonGroup.checkedButtonId) {
                    R.id.btnMale -> "Male"
                    R.id.btnFemale -> "Female"
                    else -> "Neutral"
                }
                val personalNumber = editTexts["personalNumber"]!!.text.toString().toLong()
                val documentNumber = editTexts["documentNumber"]!!.text.toString()

                if (identity == null) {
                    try {
                        val newIdentity = community.createIdentity(givenNames, surname, placeOfBirth, dateOfBirth, nationality, gender, personalNumber, documentNumber)
                        identityStore.addIdentity(newIdentity)
                    } catch(e: Exception) {
                        Log.e("ERROR", e.toString())
                        Toast.makeText(requireContext(), "Identity couldn't be added", Toast.LENGTH_SHORT).show()
                    }finally {
                        Toast.makeText(requireContext(), "Identity added", Toast.LENGTH_SHORT).show()
                        (requireActivity() as ValueTransferMainActivity).reloadActivity()
                    }
                } else {
                    identity.content.givenNames = givenNames
                    identity.content.surname = surname
                    identity.content.placeOfBirth = placeOfBirth
                    identity.content.dateOfBirth = Date(dateOfBirth)
                    identity.content.nationality = nationality
                    identity.content.gender = gender
                    identity.content.personalNumber = personalNumber
                    identity.content.documentNumber = documentNumber

                    try {
                        identityStore.editIdentity(identity)
                    } catch(e: Exception) {
                        Log.e("ERROR", e.toString())
                        Toast.makeText(requireContext(), "Identity couldn't be updated", Toast.LENGTH_SHORT).show()
                    }finally {
                        Toast.makeText(requireContext(), "Identity updated", Toast.LENGTH_SHORT).show()
                    }
                }

                bottomSheetDialog.dismiss()
                activity?.invalidateOptionsMenu()
            }
            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
