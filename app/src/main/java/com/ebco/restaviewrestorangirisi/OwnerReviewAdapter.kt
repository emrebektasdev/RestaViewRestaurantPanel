package com.ebco.restaviewrestorangirisi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OwnerReviewAdapter(
    private var reviews: List<Review>,
    private val onReplyClick: (Review) -> Unit
) : RecyclerView.Adapter<OwnerReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvComment: TextView = itemView.findViewById(R.id.tvComment)
        val layoutReply: LinearLayout = itemView.findViewById(R.id.layoutReply)
        val tvOwnerReply: TextView = itemView.findViewById(R.id.tvOwnerReply)
        val btnReply: Button = itemView.findViewById(R.id.btnReply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_owner_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        holder.tvUserName.text = review.userName
        holder.tvDate.text = review.date
        holder.tvRating.text = "H: ${review.hygieneRating}  L: ${review.tasteRating}  F: ${review.priceRating}"
        holder.tvComment.text = review.comment

        if (review.ownerReply.isNotEmpty()) {
            // Zaten cevaplanmış
            holder.layoutReply.visibility = View.VISIBLE
            holder.tvOwnerReply.text = review.ownerReply
            holder.btnReply.text = "Cevabı Düzenle"
        } else {
            // Cevaplanmamış
            holder.layoutReply.visibility = View.GONE
            holder.btnReply.text = "Cevapla"
        }

        holder.btnReply.setOnClickListener {
            onReplyClick(review)
        }
    }

    override fun getItemCount() = reviews.size

    fun updateList(newList: List<Review>) {
        reviews = newList
        notifyDataSetChanged()
    }
}
