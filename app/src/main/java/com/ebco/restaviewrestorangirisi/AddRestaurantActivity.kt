package com.ebco.restaviewrestorangirisi

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ebco.restaviewrestorangirisi.databinding.ActivityAddRestaurantBinding

class AddRestaurantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRestaurantBinding
    private val firestoreHelper = RestaViewFirestoreHelper()
    
    // Seçilen resmin URI'sini tutacak değişken
    private var selectedImageUri: Uri? = null
    
    private val cities = listOf("İstanbul", "İzmir", "Çanakkale", "Tokat", "Konya", "Ankara", "Bursa")

    // Galeri başlatıcı
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.setImageURI(uri) // Önizleme göster
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRestaurantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCitySpinner()

        binding.btnSelectImage.setOnClickListener {
            // Galeriyi aç (Sadece resimler)
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveRestaurant()
        }
    }

    private fun setupCitySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCity.adapter = adapter
    }

    private fun saveRestaurant() {
        val name = binding.etName.text.toString()
        val desc = binding.etDesc.text.toString()
        val city = binding.spinnerCity.selectedItem.toString()
        val address = binding.etAddress.text.toString()
        val tableCountStr = binding.etTableCount.text.toString()

        if (name.isEmpty() || desc.isEmpty() || address.isEmpty() || tableCountStr.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        val tableCount = tableCountStr.toIntOrNull() ?: 10
        binding.btnSave.isEnabled = false // Çift tıklamayı önle
        binding.btnSave.text = "Kaydediliyor..."

        if (selectedImageUri != null) {
            // Resim seçildiyse önce yükle
            firestoreHelper.uploadImage(selectedImageUri!!,
                onSuccess = { imageUrl ->
                    saveRestaurantToFirestore(name, desc, city, address, tableCount, imageUrl)
                },
                onFailure = {
                    Toast.makeText(this, "Resim yüklenemedi: ${it.message}", Toast.LENGTH_LONG).show()
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Kaydet ve Yayınla"
                }
            )
        } else {
            // Resim yoksa varsayılan ile devam et
            saveRestaurantToFirestore(name, desc, city, address, tableCount, "")
        }
    }

    private fun saveRestaurantToFirestore(name: String, desc: String, city: String, address: String, tableCount: Int, imageUrl: String) {
        val newRestaurant = Restaurant(
            name = name,
            description = desc,
            city = city,
            address = address,
            tableCount = tableCount,
            imageUrl = imageUrl, // Varsa URL, yoksa boş string
            imageResId = if (imageUrl.isEmpty()) 2131165300 else 0 // URL varsa ResId 0 olsun
        )

        firestoreHelper.addRestaurant(newRestaurant,
            onSuccess = {
                Toast.makeText(this, "Restoran Başarıyla Eklendi!", Toast.LENGTH_LONG).show()
                finish()
            },
            onFailure = {
                Toast.makeText(this, "Hata: ${it.message}", Toast.LENGTH_LONG).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Kaydet ve Yayınla"
            }
        )
    }
}