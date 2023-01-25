package nl.tudelft.trustchain.literaturedao.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.literaturedao.LiteratureDaoActivity
import nl.tudelft.trustchain.literaturedao.R
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import nl.tudelft.trustchain.literaturedao.model.remote_search.SearchResult
import nl.tudelft.trustchain.literaturedao.model.remote_search.SearchResultList
import nl.tudelft.trustchain.literaturedao.ui.adapters.RemoteSearchResultAdapter


class RemoteSearchFragment : Fragment(R.layout.fragment_remote_search) {

    val results: MutableList<SearchResult> = mutableListOf()
    val adapter: RemoteSearchResultAdapter = RemoteSearchResultAdapter(results)
    lateinit var progressBar: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        val view: View = inflater.inflate(R.layout.fragment_remote_search, container, false)

        //inside Fragment
        val job = Job()
        CoroutineScope(Dispatchers.Main + job)

        (context as LiteratureDaoActivity).setRemoteSearchFragment(this)

        val searchBar: SearchView = view.findViewById(R.id.remote_search_bar) as SearchView

        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.i("litdao", "perform remote search with: " + query)
                if (!query.isNullOrBlank()) {
                    // TODO: Fix for lower Android API levels.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        remoteSeach(query)
                    } else {
                        throw NotImplementedError("Remote search not implemented for Android versions below Oreo")
                    }
                    return true
                }
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                Log.i("litdao", "remote search text changed to: " + query)
                return true
            }
        })

        val recViewItems = view.findViewById<RecyclerView>(R.id.remote_search_results);
        progressBar = view.findViewById(R.id.remote_search_loading)

        recViewItems.layoutManager = LinearLayoutManager(context)
        recViewItems.adapter = adapter

        // Inflate the layout for this fragment
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun remoteSeach(query: String) {
        // send to peers
        // TODO: Fix for lower Android API levels.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IPv8Android.getInstance().getOverlay<LiteratureCommunity>()!!
                .broadcastSearchQuery(query)
        } else {
            throw NotImplementedError("Not implemented for API < 24")
        }

        results.clear()
        adapter.refresh()

        progressBar.visibility = View.VISIBLE

    }

    fun updateSearchResults(newResults: SearchResultList) {
        // access UI and append results to some view
        Log.d("litdao", "update remote search results with:" + newResults.toString())

        progressBar.visibility = View.GONE

        for (r: SearchResult in newResults.results) {
            if (!results.contains(r)) {
                results.add(r)
            }
        }

        results.sortBy { it.score }
        adapter.refresh()
    }

}
