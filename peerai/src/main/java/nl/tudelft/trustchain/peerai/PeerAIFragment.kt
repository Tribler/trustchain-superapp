package nl.tudelft.trustchain.peerai

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.RequiresApi
import android.widget.*
import androidx.core.net.toUri
import kotlinx.android.synthetic.main.fragment_peer_a_i.results
import kotlinx.android.synthetic.main.fragment_peer_a_i.searchview
import nl.tudelft.trustchain.common.ui.BaseFragment
import org.apache.commons.text.similarity.CosineSimilarity
import com.squareup.picasso.Picasso
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

import java.io.File
import java.io.IOException
import java.lang.Math.sqrt
import java.util.ArrayList

data class Album(val artist: String, val title: String, val year: String, val tags: List<String>, val magnet: String, val songs: List<String>, val payment: String, val author_image: String,val artwork: String,val author_description: String, val author_upcoming: List<Event>)

data class Event(val context: String, val type: String, val startDate:String, val offers:String, val name:String, val location: Location);
data class Location(val type: String, val addressLocality:String)

class PeerAIFragment : BaseFragment(R.layout.fragment_peer_a_i) {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        // on below line we are initializing adapter for our list view.
        val view: View = inflater.inflate(R.layout.fragment_peer_a_i, container, false)

        view.findViewById<ListView>(R.id.results).adapter = AlbumAdapter(
            requireContext(),
            emptyList<Album>()
            )


        view.findViewById<SearchView>(R.id.searchview).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // on below line we are checking
                // if query exist or not.


                return false
            }

            @RequiresApi(Build.VERSION_CODES.N)
            override fun onQueryTextChange(newText: String): Boolean {
                // if query text is change in that case we
                // are filtering our adapter with
                // new text on below line.


                val list = searchAlbums(newText);

                val adapter = AlbumAdapter(requireContext(), list)
                view.findViewById<ListView>(R.id.results).adapter = adapter;
                /*view.findViewById<ListView>(R.id.results).adapter = ArrayAdapter<String?>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    listOf("sdas test")
                )
                */


                (view.findViewById<ListView>(R.id.results).adapter as AlbumAdapter).notifyDataSetChanged();
                return false
            }
        });


        // on below line we are adding on query
        // listener for our search view.



        return view;
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


    fun calculateCosineSimilarity(query: List<String>, document: List<String>): Double {
        val queryTermFrequency = query.groupingBy { it }.eachCount()
        val documentTermFrequency = document.groupingBy { it }.eachCount()

        val dotProduct = queryTermFrequency.entries.sumOf { (term, frequency) ->
            (frequency * (documentTermFrequency[term] ?: 0))
        }

        val queryVectorLength = sqrt(queryTermFrequency.values.sumOf { it * it }.toDouble())
        val documentVectorLength = sqrt(documentTermFrequency.values.sumOf { it * it }.toDouble())

        return if (queryVectorLength > 0 && documentVectorLength > 0) {
            dotProduct / (queryVectorLength * documentVectorLength)
        } else {
            0.0
        }
    }

    fun searchAlbums(query: String): List<Album> {
        val queryTerms = query.toLowerCase().split(" ");
        val threshold = 0.15;

        this.context?.let {
            val albums = loadJsonFromAsset(it.applicationContext);
            val list =  albums.filter { album ->
                val documentTerms = (album.artist + " " + album.title + " " + album.tags.joinToString(" ") + " " +
                    album.songs.joinToString(" ")).toLowerCase().split(" ")
                val similarityScore = calculateCosineSimilarity(queryTerms, documentTerms)
                similarityScore >= threshold
            }

            return list;
        }
        return emptyList();
    }


    fun loadJsonFromAsset(context: Context): List<Album> {
        val jsonFileString = context.assets.open("scraped_data_02.json").bufferedReader().use {
            it.readText()
        }

        val gson = Gson()
        return gson.fromJson(jsonFileString, Array<Album>::class.java).toList()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun findMostSimilarItems(query: String, threshold: Double): List<String> {
        /*
        val cosine = CosineSimilarity()


        // Create term frequency vector for query
        val queryVec = createTermFrequencyVector(query)

        // Create list of items with their cosine similarity scores
        val scores = items.map { item ->
            val itemVec = createTermFrequencyVector(item)
            val score = cosine.cosineSimilarity(queryVec, itemVec)
            Pair(item, score)
        }

        // Sort the list of items by their cosine similarity scores in descending order
        val sortedScores = scores.sortedByDescending { it.second }

        // Extract the list of items from the sorted list of item scores that meet the threshold
        val sortedItems = sortedScores.filter { it.second >= threshold }.map { it.first }

        return sortedItems*/

        return  listOf("asasd");
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun createTermFrequencyVector(str: String): MutableMap<CharSequence, Int> {
        val vector = mutableMapOf<CharSequence, Int>()

        // Split the string into words and count their occurrences
        for (word in str.toLowerCase().split(" ")) {
            vector[word] = vector.getOrDefault(word, 0) + 1
        }

        return vector
    }

    //Class MyAdapter
    class AlbumAdapter(private val context: Context, private val arrayList: List<Album>) : BaseAdapter() {
        private lateinit var titleTextView: TextView
        private lateinit var authorTextView: TextView
        private lateinit var albumImageView: ImageView
        override fun getCount(): Int {
            return arrayList.size
        }
        override fun getItem(position: Int): Any {
            return position
        }
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            convertView = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false)


            convertView.setOnClickListener {
                val album = arrayList[position]
                showAuthorDescriptionDialog(context, album)
            }

            titleTextView = convertView.findViewById(R.id.titleTextView)
            authorTextView = convertView.findViewById(R.id.authorTextView)
            albumImageView= convertView.findViewById(R.id.albumImageView)

            titleTextView.text = " " + arrayList[position].title
            authorTextView.text = " " + arrayList[position].artist

            if (arrayList[position].artwork != null && arrayList[position].artwork.isNotEmpty()) {
                //albumImageView.setImageURI(arrayList[position].author_image.toUri())

                Picasso.get().load(arrayList[position].artwork).into(albumImageView)
            }

            return convertView
        }
        private fun showAuthorDescriptionDialog(context: Context, album: Album) {
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.album_info)

            val authorDescriptionTextView: TextView = dialog.findViewById(R.id.authorDescriptionTextView)
            val authorImageView: ImageView = dialog.findViewById(R.id.authorImageView)
            val magnetTextView: TextView = dialog.findViewById(R.id.magnetTextView)
            val yearTextView: TextView = dialog.findViewById(R.id.yearTextView)


            if (album.author_image != null && album.author_image.isNotEmpty()) {
                //albumImageView.setImageURI(arrayList[position].author_image.toUri())

                Picasso.get().load(album.author_image).into(authorImageView)
            }
            magnetTextView.text = album.magnet
            authorDescriptionTextView.text = album.author_description
            yearTextView.text = album.year

            dialog.show()
        }

    }

}
