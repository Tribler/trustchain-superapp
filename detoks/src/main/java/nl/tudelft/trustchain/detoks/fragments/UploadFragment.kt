package nl.tudelft.trustchain.detoks.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.DeToksFragment
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.adapters.TabBarAdapter

class UploadFragment : Fragment() {




    override fun onResume() {
        super.onResume()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val permission = ContextCompat.checkSelfPermission(this.requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.requireActivity(),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    111)

        }
        mainPart()


    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun mainPart(){
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if(uri!=null){
                TorrentManager.getInstance(this.requireContext()).createTorrentInfo(uri, this.requireContext())
                Toast.makeText(this.requireContext(), "Successfully uploaded", Toast.LENGTH_LONG).show()
            }



        }
        getContent.launch("video/*")

    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            111 -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d("AndroidRuntime", "REJECTED :(")

                } else {
                    mainPart()
                }
            }
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_upload, container, false)
    }
}
