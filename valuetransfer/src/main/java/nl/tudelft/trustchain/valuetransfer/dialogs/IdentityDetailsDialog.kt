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
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.Identity
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

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_add, null)

            val givenNamesView = view.findViewById<EditText>(R.id.etGivenNames)
            val surnameView = view.findViewById<EditText>(R.id.etSurname)
            val placeOfBirthView = view.findViewById<EditText>(R.id.etPlaceOfBirth)
            val dateOfBirthView = view.findViewById<EditText>(R.id.etDateOfBirth)
            val nationalityView = view.findViewById<EditText>(R.id.etNationality)
            val personalNumberView = view.findViewById<EditText>(R.id.etPersonalNumber)
            val documentNumberView = view.findViewById<EditText>(R.id.etDocumentNumber)
            val saveButton = view.findViewById<Button>(R.id.btnSavePersonalIdentity)
            saveButton.isEnabled = true

            val genderButtonGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.btnGenderGroup)

            if (identity != null) {
                givenNamesView.setText(identity.content.givenNames)
                surnameView.setText(identity.content.surname)
                placeOfBirthView.setText(identity.content.placeOfBirth)
                dateOfBirthView.setText(SimpleDateFormat("MMMM d, yyyy").format(identity.content.dateOfBirth))
                val day = SimpleDateFormat("d").format(identity.content.dateOfBirth).toInt()
                val month = SimpleDateFormat("M").format(identity.content.dateOfBirth).toInt()
                val year = SimpleDateFormat("yyyy").format(identity.content.dateOfBirth).toInt()
                cal.set(year, month - 1, day)

                nationalityView.setText(identity.content.nationality)
                personalNumberView.setText(identity.content.personalNumber.toString())
                documentNumberView.setText(identity.content.documentNumber)

                when(identity.content.gender) {
                    "Male" -> {
                        view.findViewById<MaterialButton>(R.id.btnMale).isChecked = true
                        view.findViewById<Button>(R.id.btnMale).setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryDarkValueTransfer
                            )
                        )
                        view.findViewById<Button>(R.id.btnMale).setTextColor(Color.WHITE)
                    }
                    "Female" -> {
                        view.findViewById<MaterialButton>(R.id.btnFemale).isChecked = true
                        view.findViewById<Button>(R.id.btnFemale).setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryDarkValueTransfer
                            )
                        )
                        view.findViewById<Button>(R.id.btnFemale).setTextColor(Color.WHITE)
                    }
                }
            }

            genderButtonGroup.addOnButtonCheckedListener(MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    if (checkedId == R.id.btnMale) {
                        Log.d("CLICKED", "MALE")
                        view.findViewById<Button>(R.id.btnMale).setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryDarkValueTransfer
                            )
                        )
                        view.findViewById<Button>(R.id.btnMale).setTextColor(Color.WHITE)
                        view.findViewById<Button>(R.id.btnFemale).setBackgroundColor(Color.WHITE)
                        view.findViewById<Button>(R.id.btnFemale).setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryValueTransfer
                            )
                        )
                    } else if(checkedId == R.id.btnFemale) {
                        Log.d("CLICKED", "FEMALE")
                        view.findViewById<Button>(R.id.btnFemale).setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryDarkValueTransfer
                            )
                        )
                        view.findViewById<Button>(R.id.btnFemale).setTextColor(Color.WHITE)
                        view.findViewById<Button>(R.id.btnMale).setBackgroundColor(Color.WHITE)
                        view.findViewById<Button>(R.id.btnMale).setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimaryValueTransfer
                            )
                        )
                    }
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
                    dateOfBirthView.setText(date)
                }
            }

            dateOfBirthView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    DatePickerDialog(requireContext(), R.style.DatePickerDialogTheme, dateSetListener, cal.get(Calendar.YEAR), cal.get(
                        Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }
            })

            saveButton.setOnClickListener {
                val givenNames = givenNamesView.text.toString()
                val surname = surnameView.text.toString()
                val placeOfBirth = placeOfBirthView.text.toString()
                val dateOfBirth = (SimpleDateFormat("MMMM d, yyyy").parse(dateOfBirthView.text.toString()) as Date).time
                val nationality = nationalityView.text.toString()
                val gender = when(genderButtonGroup.checkedButtonId) {
                    R.id.btnMale -> "Male"
                    R.id.btnFemale -> "Female"
                    else -> "Neutral"
                }
                val personalNumber = personalNumberView.text.toString().toLong()
                val documentNumber = documentNumberView.text.toString()

                if (identity == null) {
                    try {
                        val newIdentity = community.createPersonalIdentity(givenNames, surname, placeOfBirth, dateOfBirth, nationality, gender, personalNumber, documentNumber)
                        identityStore.addIdentity(newIdentity)
                        Toast.makeText(requireContext(), "Personal identity added", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(nl.tudelft.trustchain.valuetransfer.R.id.action_identityFragment_to_walletOverViewFragment)
                    } catch(e: Exception) {
                        Log.e("ERROR", e.toString())
                        Toast.makeText(requireContext(), "Personal identity couldn't be added", Toast.LENGTH_SHORT).show()
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
                        identityStore.editPersonalIdentity(identity)
                        Toast.makeText(requireContext(), "Personal identity updated", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(nl.tudelft.trustchain.valuetransfer.R.id.action_identityFragment_to_walletOverViewFragment)
                    } catch(e: Exception) {
                        Log.e("ERROR", e.toString())
                        Toast.makeText(requireContext(), "Personal identity couldn't be updated", Toast.LENGTH_SHORT).show()
                    }
                }

                bottomSheetDialog.dismiss()
                activity?.invalidateOptionsMenu()
            }
            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
