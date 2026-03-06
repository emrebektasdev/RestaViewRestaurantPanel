package com.ebco.restaviewrestorangirisi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ebco.restaviewrestorangirisi.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firestoreHelper = RestaViewFirestoreHelper()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: MyRestaurantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUserInfo()
        setupRecyclerView()
        
        binding.fabAddRestaurant.setOnClickListener {
            startActivity(Intent(this, AddRestaurantActivity::class.java))
        }

        // Çıkış Yap Butonu
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val email = auth.currentUser?.email ?: ""
        
        // Önce "owners" koleksiyonuna bak, yoksa "users"a bak (eski kayıtlar için)
        db.collection("owners").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: email.substringBefore("@")
                    binding.tvWelcome.text = "Hoşgeldiniz, $name"
                } else {
                    // owners'da yoksa users'a bak (Eski kayıtlar silinmesin diye)
                    db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                         if (userDoc.exists()) {
                             val name = userDoc.getString("name") ?: email.substringBefore("@")
                             binding.tvWelcome.text = "Hoşgeldiniz, $name"
                         } else {
                             binding.tvWelcome.text = "Hoşgeldiniz, ${email.substringBefore("@")}"
                         }
                    }
                }
            }
            .addOnFailureListener {
                 binding.tvWelcome.text = "Hoşgeldiniz, ${email.substringBefore("@")}"
            }
    }

    override fun onResume() {
        super.onResume()
        loadRestaurants()
    }

    private fun setupRecyclerView() {
        adapter = MyRestaurantAdapter(emptyList()) { restaurant ->
            // Yönet butonuna tıklandığında
            val intent = Intent(this, ManageRestaurantActivity::class.java)
            intent.putExtra("RESTAURANT_ID", restaurant.id)
            intent.putExtra("RESTAURANT_NAME", restaurant.name)
            startActivity(intent)
        }
        binding.rvRestaurants.layoutManager = LinearLayoutManager(this)
        binding.rvRestaurants.adapter = adapter
    }

    private fun loadRestaurants() {
        firestoreHelper.getMyRestaurants(
            onSuccess = { list ->
                adapter.updateList(list)
            },
            onFailure = {
                Toast.makeText(this, "Hata: ${it.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
}