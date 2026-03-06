package com.ebco.restaviewrestorangirisi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ebco.restaviewrestorangirisi.databinding.ItemRequestBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RequestAdapter(
    private var requests: List<CustomerRequest>,
    private val onCompleteClick: (CustomerRequest) -> Unit
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(private val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: CustomerRequest) {
            binding.tvTableName.text = request.tableName
            binding.tvRequestType.text = request.requestType.uppercase()
            
            if (request.note.isNotEmpty()) {
                binding.tvNote.text = "Not: ${request.note}"
                binding.tvNote.visibility = View.VISIBLE
            } else {
                binding.tvNote.visibility = View.GONE
            }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.tvTime.text = timeFormat.format(Date(request.timestamp))

            binding.btnComplete.setOnClickListener {
                onCompleteClick(request)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    fun updateList(newList: List<CustomerRequest>) {
        requests = newList
        notifyDataSetChanged()
    }
}


