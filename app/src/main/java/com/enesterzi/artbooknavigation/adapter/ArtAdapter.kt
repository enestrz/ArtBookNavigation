package com.enesterzi.artbooknavigation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.enesterzi.artbooknavigation.databinding.RecyclerRowBinding
import com.enesterzi.artbooknavigation.model.Art
import com.enesterzi.artbooknavigation.view.ListFragmentDirections

class ArtAdapter(val artList: List<Art>) : RecyclerView.Adapter<ArtAdapter.ArtHolder>() {

    class  ArtHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtHolder(binding)
    }

    override fun getItemCount(): Int {
        return artList.size
    }

    override fun onBindViewHolder(holder: ArtHolder, position: Int) {
        holder.binding.recyclerText.text = artList[position].artName
        holder.itemView.setOnClickListener {
            val action = ListFragmentDirections.actionListFragmentToUploadFragment("old", id = artList[position].id)
            Navigation.findNavController(it).navigate(action)
        }
    }
}