package com.ebco.restaviewrestorangirisi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ebco.restaviewrestorangirisi.databinding.ActivityManageRestaurantBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ManageRestaurantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageRestaurantBinding
    private var restaurantId: String = ""
    private val firestoreHelper = RestaViewFirestoreHelper()
    
    private lateinit var requestAdapter: RequestAdapter
    private lateinit var reviewAdapter: OwnerReviewAdapter
    
    private var requestsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRestaurantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantId = intent.getStringExtra("RESTAURANT_ID") ?: ""
        val restaurantName = intent.getStringExtra("RESTAURANT_NAME") ?: "Restoran"

        binding.tvRestaurantName.text = restaurantName
        binding.tvTitle.text = "$restaurantName Yönetimi"

        setupRecyclerViews()
        setupButtons()

        if (restaurantId.isNotEmpty()) {
            listenForRequests()
            loadReviews()
        }
    }

    private fun setupButtons() {
        // Çıkış Yap Butonu
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Sekme Değişimi
        binding.btnTabRequests.setOnClickListener {
            showRequestsTab()
        }

        binding.btnTabReviews.setOnClickListener {
            showReviewsTab()
        }
    }

    private fun showRequestsTab() {
        binding.rvRequests.visibility = View.VISIBLE
        binding.rvReviews.visibility = View.GONE
        
        // Buton renklerini güncelle
        binding.btnTabRequests.backgroundTintList = getMyColorStateList(android.R.color.holo_orange_light) // Turuncu
        binding.btnTabRequests.setTextColor(getColor(android.R.color.white))
        
        binding.btnTabReviews.backgroundTintList = getMyColorStateList(android.R.color.darker_gray)
        binding.btnTabReviews.setTextColor(getColor(android.R.color.black))
    }

    private fun showReviewsTab() {
        binding.rvRequests.visibility = View.GONE
        binding.rvReviews.visibility = View.VISIBLE
        
        binding.btnTabReviews.backgroundTintList = getMyColorStateList(android.R.color.holo_orange_light) // Aktif renk
        binding.btnTabReviews.setTextColor(getColor(android.R.color.white))
        
        binding.btnTabRequests.backgroundTintList = getMyColorStateList(android.R.color.darker_gray)
        binding.btnTabRequests.setTextColor(getColor(android.R.color.black))
        
        // Yorumları tekrar yükle
        loadReviews()
    }
    
    // Yardımcı: Renk state list oluşturucu (API seviyesine göre basit çözüm)
    private fun getMyColorStateList(colorRes: Int): android.content.res.ColorStateList {
        return android.content.res.ColorStateList.valueOf(getColor(colorRes))
    }

    private fun setupRecyclerViews() {
        // İstekler
        requestAdapter = RequestAdapter(emptyList()) { request ->
            completeRequest(request)
        }
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRequests.adapter = requestAdapter

        // Yorumlar
        reviewAdapter = OwnerReviewAdapter(emptyList()) { review ->
            showReplyDialog(review)
        }
        binding.rvReviews.layoutManager = LinearLayoutManager(this)
        binding.rvReviews.adapter = reviewAdapter
    }

    private fun listenForRequests() {
        requestsListener = firestoreHelper.listenForRequests(restaurantId) { requests ->
            requestAdapter.updateList(requests)
            if (requests.isNotEmpty()) {
                // Burada bildirim de verilebilir
            }
        }
    }

    private fun completeRequest(request: CustomerRequest) {
        firestoreHelper.completeRequest(request.id) {
            Toast.makeText(this, "İstek tamamlandı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReviews() {
        binding.progressBar.visibility = View.VISIBLE
        firestoreHelper.getReviews(restaurantId, 
            onSuccess = { reviews ->
                binding.progressBar.visibility = View.GONE
                // En yeni yorum en üstte
                reviewAdapter.updateList(reviews.sortedByDescending { it.date }) 
            },
            onFailure = {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Yorumlar yüklenemedi", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showReplyDialog(review: Review) {
        val input = EditText(this)
        input.hint = "Cevabınızı yazın..."
        input.setText(review.ownerReply) // Varsa eski cevabı koy

        AlertDialog.Builder(this)
            .setTitle("${review.userName} kullanıcısına cevap ver")
            .setView(input)
            .setPositiveButton("Gönder") { _, _ ->
                val replyText = input.text.toString()
                if (replyText.isNotEmpty()) {
                    sendReply(review.id, replyText)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun sendReply(reviewId: String, reply: String) {
        firestoreHelper.replyToReview(restaurantId, reviewId, reply,
            onSuccess = {
                Toast.makeText(this, "Cevabınız kaydedildi!", Toast.LENGTH_SHORT).show()
                loadReviews() // Listeyi yenile
            },
            onFailure = {
                Toast.makeText(this, "Hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        requestsListener?.remove()
    }
}