package nl.tudelft.trustchain.literaturedao

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.literaturedao.data_types.Literature
import nl.tudelft.trustchain.literaturedao.data_types.LocalData
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MyLiteratureFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyLiteratureFragment : Fragment(R.layout.fragment_my_literature) {

    fun loadLocalData(): LocalData{
        // Load local data
        var fileInputStream: FileInputStream? = null


        try {
            try {
                fileInputStream = context?.openFileInput("localData.json")
            } catch (e: FileNotFoundException) {
                context?.openFileOutput("localData.json", Context.MODE_PRIVATE).use { output ->
                    output?.write(
                        Json.encodeToString(LocalData(mutableListOf<Literature>())).toByteArray()
                    )
                }
                fileInputStream = context?.openFileInput("localData.json")
            }
            var inputStreamReader: InputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder: StringBuilder = StringBuilder()
            var text: String? = null
            while ({ text = bufferedReader.readLine(); text }() != null) {
                stringBuilder.append(text)
            }
            val localData: LocalData = Json.decodeFromString<LocalData>(stringBuilder.toString())
            return localData
        } catch (e: Exception) {
            return LocalData(mutableListOf<Literature>());
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("litDao", loadLocalData().toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view : View =  inflater.inflate(R.layout.fragment_my_literature, container, false)


        val json = loadLocalData();

        val recViewItems = view.findViewById<RecyclerView>(R.id.recycler_view_items);



        recViewItems.layoutManager = LinearLayoutManager(context )
        recViewItems.adapter = ItemAdapter(json.content);

        if (json.content.size == 0) {
            view.findViewById<TextView>(R.id.no_local_results).visibility = View.VISIBLE;
        }


        // Inflate the layout for this fragment
        return view;
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data);
    }

}
