package nl.tudelft.trustchain.valuetransfer.ui

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentIdentityBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.BusinessIdentity
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.IdentityItemRenderer
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class IdentityFragment : BaseFragment(R.layout.fragment_identity) {

    private val binding by viewBinding(FragmentIdentityBinding::bind)
    private val adapterPersonal = ItemAdapter()
    private val adapterBusiness = ItemAdapter()

    private var cal = Calendar.getInstance()

    private val itemsPersonal: LiveData<List<Item>> by lazy {
        store.getAllPersonalIdentities().map { identities ->
            createItems(identities)
        }.asLiveData()
    }

    private val itemsBusiness: LiveData<List<Item>> by lazy {
        store.getAllBusinessIdentities().map { identities ->
            createItems(identities)
        }.asLiveData()
    }

    private val store by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private fun getCommunity() : IdentityCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("IdentityCommunity is not configured")
    }

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterPersonal.registerRenderer(
            IdentityItemRenderer(
                1,
                {
                    Log.d("LONG", "CLICK")

                    showOptions("Personal", it)
                }, {
                    Log.d("CLICKED", "QR Code for personal item "+ it.publicKey.keyToBin().toHex())
                    dialogQRCode("Personal Public Key", "Show QR-code to other party", it.publicKey.keyToBin().toHex())
                }, {
                    addToClipboard(it.publicKey.keyToBin().toHex(), "Public Key")
                    Toast.makeText(this.context,"Public key copied to clipboard",Toast.LENGTH_SHORT).show()
                    Log.d("CLICKED", "Copy Public Key for personal item "+ it.publicKey.keyToBin().toHex())
                }
            )
        )

        itemsPersonal.observe(
            this,
            Observer {
                adapterPersonal.updateItems(it)

                if(store.hasPersonalIdentity()) {
                    binding.tvNoPersonalIdentity.visibility = View.GONE
                    binding.btnAddPersonalIdentity.visibility = View.GONE
                }else{
                    binding.tvNoPersonalIdentity.visibility = View.VISIBLE
                    binding.btnAddPersonalIdentity.visibility = View.VISIBLE
                }
            }
        )

        adapterBusiness.registerRenderer(
            IdentityItemRenderer(
            2,
                {
                    Log.d("LONG", "CLICK")

                    showOptions("Business", it)

                }, {
                    dialogQRCode("Business Public Key", "Show QR-code to other party", it.publicKey.keyToBin().toHex())
                    Log.d("CLICKED", "QR Code for business item "+ it.publicKey.keyToBin().toHex())
                }, {
                    addToClipboard(it.publicKey.keyToBin().toHex(), "Public Key")
                    Toast.makeText(this.context,"Public key copied to clipboard",Toast.LENGTH_SHORT).show()
                    Log.d("CLICKED", "Copy Public Key for personal item "+ it.publicKey.keyToBin().toHex())
                }
            )
        )

        itemsBusiness.observe(
            this,
            Observer {
                adapterBusiness.updateItems(it)

                if(store.hasBusinessIdentity()) {
                    binding.tvNoBusinessIdentities.visibility = View.GONE
                }else{
                    binding.tvNoBusinessIdentities.visibility = View.VISIBLE
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddPersonalIdentity.setOnClickListener {
            Toast.makeText(this.context, "Add personal identity clicked", Toast.LENGTH_SHORT).show()

            dialogBottom(null)
        }

        binding.btnAddBusinessIdentity.setOnClickListener {
            Toast.makeText(this.context, "Add business identity clicked", Toast.LENGTH_SHORT).show()

            dialogCreateIdentity("Business", null)
        }

        binding.rvPersonalIdentities.adapter = adapterPersonal
        binding.rvPersonalIdentities.layoutManager = LinearLayoutManager(context)
        binding.rvPersonalIdentities.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        binding.rvBusinessIdentities.adapter = adapterBusiness
        binding.rvBusinessIdentities.layoutManager = LinearLayoutManager(context)
        binding.rvBusinessIdentities.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )
    }

    private fun showOptions(type: String, identity: Identity) {
        val actions = arrayOf("Edit", "Remove")
        AlertDialog.Builder(requireContext())
            .setItems(actions) { _, action ->
                when(action) {
                    0 -> {
                        dialogBottom(identity)
                    }
                    1 -> {
                        store.deleteIdentity(identity)
                        Toast.makeText(this.context, "Removed $type identity: ${identity.publicKey}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun createBitmap(attributes: Map<String,String>): Bitmap {
        val data = JSONObject()
        for((key, value) in attributes) {
            data.put(key, value)
        }

        return qrCodeUtils.createQR(data.toString())!!
    }

    private fun dialogBottom(identity: Identity?) {
        val bottomSheetDialog = BottomSheetDialog(requireContext(),R.style.BaseBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.dialog_create_personal_identity, null)

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
            givenNamesView.setText((identity.content as PersonalIdentity).givenNames)
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
                    view.findViewById<Button>(R.id.btnMale).setBackgroundColor(getColor(requireContext(),R.color.colorPrimaryDarkValueTransfer))
                    view.findViewById<Button>(R.id.btnMale).setTextColor(Color.WHITE)
                }
                "Female" -> {
                    view.findViewById<MaterialButton>(R.id.btnFemale).isChecked = true
                    view.findViewById<Button>(R.id.btnFemale).setBackgroundColor(getColor(requireContext(),R.color.colorPrimaryDarkValueTransfer))
                    view.findViewById<Button>(R.id.btnFemale).setTextColor(Color.WHITE)
                }
            }
        }

        genderButtonGroup.addOnButtonCheckedListener(MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnMale) {
                    Log.d("CLICKED", "MALE")
                    view.findViewById<Button>(R.id.btnMale).setBackgroundColor(getColor(requireContext(),R.color.colorPrimaryDarkValueTransfer))
                    view.findViewById<Button>(R.id.btnMale).setTextColor(Color.WHITE)
                    view.findViewById<Button>(R.id.btnFemale).setBackgroundColor(Color.WHITE)
                    view.findViewById<Button>(R.id.btnFemale).setTextColor(getColor(requireContext(),R.color.colorPrimaryValueTransfer))
                } else if(checkedId == R.id.btnFemale) {
                    Log.d("CLICKED", "FEMALE")
                    view.findViewById<Button>(R.id.btnFemale).setBackgroundColor(getColor(requireContext(),R.color.colorPrimaryDarkValueTransfer))
                    view.findViewById<Button>(R.id.btnFemale).setTextColor(Color.WHITE)
                    view.findViewById<Button>(R.id.btnMale).setBackgroundColor(Color.WHITE)
                    view.findViewById<Button>(R.id.btnMale).setTextColor(getColor(requireContext(),R.color.colorPrimaryValueTransfer))
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
                DatePickerDialog(requireContext(), dateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
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
                val newIdentity = getCommunity().createPersonalIdentity(givenNames, surname, placeOfBirth, dateOfBirth, nationality, gender, personalNumber, documentNumber)
                store.addIdentity(newIdentity)
                Toast.makeText(requireContext(), "Personal identity added", Toast.LENGTH_SHORT).show()
            } else {
                (identity.content as PersonalIdentity).givenNames = givenNames
                identity.content.surname = surname
                identity.content.placeOfBirth = placeOfBirth
                identity.content.dateOfBirth = Date(dateOfBirth)
                identity.content.nationality = nationality
                identity.content.gender = gender
                identity.content.personalNumber = personalNumber
                identity.content.documentNumber = documentNumber

                store.editPersonalIdentity(identity)
                Toast.makeText(requireContext(), "Personal identity updated", Toast.LENGTH_SHORT)
                    .show()
            }

            bottomSheetDialog.dismiss()
        }
    }

    private fun dialogCreateIdentity(type: String, identity: Identity?) {

        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)

        if(type == "Personal") {
            val view = layoutInflater.inflate(R.layout.dialog_create_personal_identity, null)

            val givenNamesView = view.findViewById<EditText>(R.id.etGivenNames)
            val surnameView = view.findViewById<EditText>(R.id.etSurname)
            val placeOfBirthView = view.findViewById<EditText>(R.id.etPlaceOfBirth)
            val dateOfBirthView = view.findViewById<EditText>(R.id.etDateOfBirth)
            val nationalityView = view.findViewById<EditText>(R.id.etNationality)
            val genderView = view.findViewById<AutoCompleteTextView>(R.id.ddGender)
            val personalNumberView = view.findViewById<EditText>(R.id.etPersonalNumber)
            val documentNumberView = view.findViewById<EditText>(R.id.etDocumentNumber)
            val saveButton = view.findViewById<Button>(R.id.btnSavePersonalIdentity)
            saveButton.isEnabled = true

            val adapter = ArrayAdapter.createFromResource(requireContext(), R.array.gender_list, android.R.layout.simple_spinner_dropdown_item)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            genderView.setAdapter(adapter)

            if(identity != null) {
                givenNamesView.setText((identity.content as PersonalIdentity).givenNames)
                surnameView.setText(identity.content.surname)
                placeOfBirthView.setText(identity.content.placeOfBirth)
                dateOfBirthView.setText(SimpleDateFormat("MMMM d, yyyy").format(identity.content.dateOfBirth))
                val day = SimpleDateFormat("d").format(identity.content.dateOfBirth).toInt()
                val month = SimpleDateFormat("M").format(identity.content.dateOfBirth).toInt()
                val year = SimpleDateFormat("yyyy").format(identity.content.dateOfBirth).toInt()
                cal.set(year, month-1, day)

                nationalityView.setText(identity.content.nationality)
                personalNumberView.setText(identity.content.personalNumber.toString())
                documentNumberView.setText(identity.content.documentNumber)

                genderView.setText(identity.content.gender, false)
            }

            builder.setView(view)
            val dialog : AlertDialog = builder.create()
            dialog.show()

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
                    DatePickerDialog(requireContext(), dateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }
            })

            saveButton.setOnClickListener {
                val givenNames = givenNamesView.text.toString()
                val surname = surnameView.text.toString()
                val placeOfBirth = placeOfBirthView.text.toString()
                val dateOfBirth = (SimpleDateFormat("MMMM d, yyyy").parse(dateOfBirthView.text.toString()) as Date).time
                val nationality = nationalityView.text.toString()
                val gender = genderView.text.toString()
                val personalNumber = personalNumberView.text.toString().toLong()
                val documentNumber = documentNumberView.text.toString()

                if(identity == null) {
                    val newIdentity = getCommunity()
                        .createPersonalIdentity(givenNames, surname, placeOfBirth, dateOfBirth, nationality, gender, personalNumber, documentNumber)
                    store.addIdentity(newIdentity)
                    Toast.makeText(requireContext(), "Personal identity added", Toast.LENGTH_SHORT).show()
                }else{
                    (identity.content as PersonalIdentity).givenNames = givenNames
                    identity.content.surname = surname
                    identity.content.placeOfBirth = placeOfBirth
                    identity.content.dateOfBirth = Date(dateOfBirth)
                    identity.content.nationality = nationality
                    identity.content.gender = gender
                    identity.content.personalNumber = personalNumber
                    identity.content.documentNumber = documentNumber

                    store.editPersonalIdentity(identity)
                    Toast.makeText(requireContext(), "Personal identity updated", Toast.LENGTH_SHORT).show()

                }


                dialog.dismiss()
            }

        }else if(type == "Business") {
            val view = layoutInflater.inflate(R.layout.dialog_create_business_identity, null)

            val companyNameView = view.findViewById<EditText>(R.id.etCompanyName)
            val residenceView = view.findViewById<EditText>(R.id.etResidence)
            val establishedView = view.findViewById<EditText>(R.id.etEstablished)
            val areaOfExpertiseView = view.findViewById<EditText>(R.id.etAreaOfExpertise)

            val saveButton = view.findViewById<Button>(R.id.btnSaveBusinessIdentity)
            saveButton.isEnabled = true

            if(identity != null) {
                companyNameView.setText((identity.content as BusinessIdentity).companyName)
                residenceView.setText(identity.content.residence)
                establishedView.setText(SimpleDateFormat("yyyy").format(identity.content.dateOfBirth))
                areaOfExpertiseView.setText(identity.content.areaOfExpertise)
            }

            builder.setView(view)
            view.setBackgroundColor(Color.TRANSPARENT)
            val dialog : AlertDialog = builder.create()

            dialog.show()

            saveButton.setOnClickListener {
                val companyName = companyNameView.text.toString()
                val residence = residenceView.text.toString()
                val established = (SimpleDateFormat("yyyy").parse(establishedView.text.toString()) as Date).time
                val areaOfExpertise = areaOfExpertiseView.text.toString()

                if(identity == null) {
                    val newIdentity = getCommunity()
                        .createBusinessIdentity(companyName, established, residence, areaOfExpertise)
                    store.addIdentity(newIdentity)
                    Toast.makeText(requireContext(), "Business identity added", Toast.LENGTH_SHORT).show()
                }else{
                    (identity.content as BusinessIdentity).companyName = companyName
                    identity.content.residence = residence
                    identity.content.dateOfBirth = Date(established)
                    identity.content.areaOfExpertise = areaOfExpertise

                    store.editBusinessIdentity(identity)
                    Toast.makeText(requireContext(), "Business identity updated", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
        }
    }

    private fun dialogQRCode(title: String, subtitle: String, publicKey: String) {

        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_qrcode, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubTitle = view.findViewById<TextView>(R.id.tvSubTitle)
        val ivQRCode = view.findViewById<ImageView>(R.id.ivQRCode)
        val btnCloseDialog = view.findViewById<Button>(R.id.btnCloseDialog)

        builder.setView(view)
        val dialog : AlertDialog = builder.create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        Handler().postDelayed(
            Runnable {
                view.findViewById<ProgressBar>(R.id.pbLoadingSpinner).visibility = View.GONE
                tvTitle.text = title
                tvSubTitle.text = subtitle
                btnCloseDialog.visibility = View.VISIBLE

                val map = mapOf(
                    "public_key" to publicKey,
                    "message" to "TEST"
                )

                ivQRCode.setImageBitmap(createBitmap(map))
            }, 100)

    }

    private fun addToClipboard(text: String, label: String) {
        val clipboard = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    private fun createItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }
}
