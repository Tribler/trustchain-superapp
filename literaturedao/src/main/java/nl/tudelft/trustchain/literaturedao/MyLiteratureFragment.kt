package nl.tudelft.trustchain.literaturedao

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.literaturedao.data_types.LocalData
import nl.tudelft.trustchain.literaturedao.utils.CacheUtil


class MyLiteratureFragment : Fragment(R.layout.fragment_my_literature) {

    fun loadLocalData(): LocalData {
        return CacheUtil(context).loadLocalData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_my_literature, container, false)

        val json = loadLocalData()

        val recViewItems = view.findViewById<RecyclerView>(R.id.recycler_view_items)

        val adapter = ItemAdapter(json.content)

        recViewItems.layoutManager = LinearLayoutManager(context)
        recViewItems.adapter = adapter

        if (json.content.size == 0) {
            view.findViewById<TextView>(R.id.no_local_results).visibility = View.VISIBLE
        }

        // Initialize binding local search.
        val localSearchView = view.findViewById<SearchView>(R.id.searchViewLit)


        localSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                // your text view here

                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                //val results = localSearch(query)
                adapter.items.clear()
                val results = CacheUtil(context).localSearch(query)

                adapter.items.addAll(results.map { it.first })
                adapter.notifyDataSetChanged()

                return true
            }
        })

        // Inflate the layout for this fragment
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") // TODO: Fix deprecation issue.
        super.onActivityResult(requestCode, resultCode, data)
    }

}
