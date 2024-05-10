package com.enesterzi.artbooknavigation.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.enesterzi.artbooknavigation.R
import com.enesterzi.artbooknavigation.adapter.ArtAdapter
import com.enesterzi.artbooknavigation.databinding.FragmentListBinding
import com.enesterzi.artbooknavigation.model.Art
import com.enesterzi.artbooknavigation.roomdb.ArtDao
import com.enesterzi.artbooknavigation.roomdb.ArtDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class ListFragment : Fragment() {

    private var _binding : FragmentListBinding? = null
    private val binding get() = _binding!!
    private val mDisposable = CompositeDisposable()
    private lateinit var artDao : ArtDao
    private lateinit var artDatabase : ArtDatabase
    private lateinit var artAdapter : ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        artDatabase = Room.databaseBuilder(requireContext(), ArtDatabase::class.java, "Arts").build()
        artDao = artDatabase.artDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.inflateMenu(R.menu.list_menu)

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_art -> {
                    val action = ListFragmentDirections.actionListFragmentToUploadFragment("new", id = -1)
                    Navigation.findNavController(view).navigate(action)
                    true
                }
                else -> false
            }
        }

        getFromSQL()

    }

     private fun getFromSQL () {
        mDisposable.add(artDao.getArtWithNameAndId()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::handleResponse))
    }

    private fun handleResponse(artList: List<Art>) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        artAdapter = ArtAdapter(artList)
        binding.recyclerView.adapter = artAdapter
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}