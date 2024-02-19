package nl.tudelft.trustchain.peerai

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.peerai.databinding.AlbumInfoBinding
import nl.tudelft.trustchain.peerai.databinding.FragmentPeerAIBinding
import nl.tudelft.trustchain.peerai.databinding.ItemAlbumBinding
import java.lang.Math.sqrt

data class Album(
    val artist: String,
    val title: String,
    val year: String,
    val tags: List<String>,
    val magnet: String,
    val songs: List<String>,
    val payment: String,
    val author_image: String,
    val artwork: String,
    val author_description: String,
    val author_upcoming: List<Event>
)

data class Event(
    val context: String,
    val type: String,
    val startDate: String,
    val offers: String,
    val name: String,
    val location: Location
)

data class Location(val type: String, val addressLocality: String)

class PeerAIFragment : BaseFragment(R.layout.fragment_peer_a_i) {
    @Suppress("ktlint:standard:property-naming") // False positive
    private var _binding: FragmentPeerAIBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeerAIBinding.inflate(inflater, container, false)
        val view = binding.root

        // on below line we are initializing adapter for our list view.
        binding.results.adapter =
            AlbumAdapter(
                requireContext(),
                emptyList()
            )

        binding.searchview.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    // on below line we are checking
                    // if query exist or not.
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    // if query text is change in that case we
                    // are filtering our adapter with
                    // new text on below line.
                    val list = searchAlbums(newText)

                    val adapter = AlbumAdapter(requireContext(), list)
                    binding.results.adapter = adapter
                    (binding.results.adapter as AlbumAdapter).notifyDataSetChanged()
                    return false
                }
            }
        )

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun calculateCosineSimilarity(
        query: List<String>,
        document: List<String>
    ): Double {
        val queryTermFrequency = query.groupingBy { it }.eachCount()
        val documentTermFrequency = document.groupingBy { it }.eachCount()

        val dotProduct =
            queryTermFrequency.entries.sumOf { (term, frequency) ->
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
        val queryTerms = query.lowercase().split(" ")
        val threshold = 0.15

        this.context?.let {
            val albums = loadJsonFromAsset(it.applicationContext)
            val list =
                albums.filter { album ->
                    val documentTerms =
                        (
                            album.artist + " " + album.title + " " + album.tags.joinToString(" ") + " " +
                                album.songs.joinToString(" ")
                        ).lowercase().split(" ")
                    val similarityScore = calculateCosineSimilarity(queryTerms, documentTerms)
                    similarityScore >= threshold
                }

            return list
        }
        return emptyList()
    }

    fun loadJsonFromAsset(context: Context): List<Album> {
        val jsonFileString =
            context.assets.open("scraped_data_02.json").bufferedReader().use {
                it.readText()
            }
        val gson = Gson()
        return gson.fromJson(jsonFileString, Array<Album>::class.java).toList()
    }

    fun createTermFrequencyVector(str: String): MutableMap<CharSequence, Int> {
        val vector = mutableMapOf<CharSequence, Int>()

        // Split the string into words and count their occurrences
        for (word in str.lowercase().split(" ")) {
            vector[word] = vector.getOrDefault(word, 0) + 1
        }

        return vector
    }

    class AlbumAdapter(private val context: Context, private val arrayList: List<Album>) :
        BaseAdapter() {
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

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View? {
            val binding =
                if (convertView != null) {
                    ItemAlbumBinding.bind(convertView)
                } else {
                    ItemAlbumBinding.inflate(LayoutInflater.from(context))
                }
            val view = binding.root

            view.setOnClickListener {
                val album = arrayList[position]
                showAuthorDescriptionDialog(context, album)
            }

            titleTextView = binding.titleTextView
            authorTextView = binding.authorTextView
            albumImageView = binding.albumImageView

            titleTextView.text = " " + arrayList[position].title
            authorTextView.text = " " + arrayList[position].artist

            if (arrayList[position].artwork.isNotEmpty()) {
                Picasso.get().load(arrayList[position].artwork).into(albumImageView)
            }

            return convertView
        }

        private fun showAuthorDescriptionDialog(
            context: Context,
            album: Album
        ) {
            val dialog = Dialog(context)
            val binding = AlbumInfoBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(binding.root)

            val authorDescriptionTextView = binding.authorDescriptionTextView
            val authorImageView = binding.authorImageView
            val magnetTextView = binding.magnetTextView
            val yearTextView = binding.yearTextView

            if (album.author_image.isNotEmpty()) {
                Picasso.get().load(album.author_image).into(authorImageView)
            }
            magnetTextView.text = album.magnet
            authorDescriptionTextView.text = album.author_description
            yearTextView.text = album.year

            dialog.show()
        }
    }
}
