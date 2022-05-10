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
import nl.tudelft.trustchain.literaturedao.data_types.LocalData
import nl.tudelft.trustchain.literaturedao.utils.CacheUtil

class MyLiteratureFragment : Fragment(R.layout.fragment_my_literature) {

    fun loadLocalData(): LocalData{
        return CacheUtil(context).loadLocalData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
