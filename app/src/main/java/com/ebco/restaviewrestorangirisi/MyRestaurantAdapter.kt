package com.ebco.restaviewrestorangirisi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ebco.restaviewrestorangirisi.databinding.ItemMyRestaurantBinding

class MyRestaurantAdapter(
    private var restaurants: List<Restaurant>,
    private val onManageClick: (Restaurant) -> Unit
) : RecyclerView.Adapter<MyRestaurantAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMyRestaurantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(restaurant: Restaurant) {
            binding.tvName.text = restaurant.name
            binding.tvCity.text = restaurant.city
            
            if (restaurant.rwScore > 0) {
                binding.tvScores.text = "RW: ${restaurant.rwScore} | H:${restaurant.hygieneScore} L:${restaurant.tasteScore} F:${restaurant.priceScore}"
            } else {
                binding.tvScores.text = "Henüz puanlanmadı (0.0)"
            }

            binding.btnManage.setOnClickListener {
                onManageClick(restaurant)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyRestaurantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(restaurants[position])
    }

    override fun getItemCount(): Int = restaurants.size

    fun updateList(newList: List<Restaurant>) {
        restaurants = newList
        notifyDataSetChanged()
    }
}


