package com.ebco.restaviewrestorangirisi

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

class RestaViewFirestoreHelper {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // Storage adresini manuel olarak veriyoruz, bu en garanti yöntemdir.
    private val storage = FirebaseStorage.getInstance("gs://restoranapp-c16f7.firebasestorage.app")
    private val restaurantsCollection = db.collection("restaurants")
    private val requestsCollection = db.collection("requests") 

    fun addRestaurant(restaurant: Restaurant, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Okunabilir ID oluştur: "restoran-adi-sehir-benzersizkod"
        val slugName = toSlug(restaurant.name)
        val slugCity = toSlug(restaurant.city)
        val uniqueCode = UUID.randomUUID().toString().substring(0, 4) // Kısa kod
        val docId = "$slugName-$slugCity-$uniqueCode"
        
        val newDoc = restaurantsCollection.document(docId)
        val restaurantWithId = restaurant.copy(id = docId, ownerId = auth.currentUser?.uid ?: "")
        
        newDoc.set(restaurantWithId)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    
    // Türkçe karakterleri ve boşlukları temizleyip URL dostu string yapar
    private fun toSlug(input: String): String {
        val nonLatin = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .toLowerCase(Locale.ENGLISH)
        return nonLatin.replace(Regex("[^a-z0-9]"), "-").replace(Regex("-+"), "-").trim('-')
    }

    fun getMyRestaurants(onSuccess: (List<Restaurant>) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        
        restaurantsCollection.whereEqualTo("ownerId", userId).get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(Restaurant::class.java)
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateRestaurantSettings(restaurantId: String, data: Map<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        restaurantsCollection.document(restaurantId).update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // YENİ: Resim Yükleme Fonksiyonu (Daha Detaylı Hata Ayıklama İçin Güncellendi)
    fun uploadImage(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val ref = storage.reference.child("restaurant_images/${UUID.randomUUID()}.jpg")
        
        // 1. Adım: Dosyayı Yükle
        ref.putFile(imageUri)
            .addOnSuccessListener {
                // Yükleme Başarılı oldu, şimdi linki iste
                ref.downloadUrl
                    .addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                    .addOnFailureListener { e ->
                        // Dosya yüklendi ama link alınamadı (Genelde Rules kaynaklıdır)
                        onFailure(Exception("Yükleme başarılı ama link alınamadı: ${e.message}"))
                    }
            }
            .addOnFailureListener { e ->
                // Yükleme hiç başlamadı veya yarıda kesildi
                onFailure(Exception("Dosya yüklenirken hata oluştu: ${e.message}"))
            }
    }

    fun listenForRequests(restaurantId: String, onUpdate: (List<CustomerRequest>) -> Unit): ListenerRegistration {
        return requestsCollection
            .whereEqualTo("restaurantId", restaurantId)
            .whereEqualTo("status", "pending") // Sadece bekleyenleri göster
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val requests = snapshot.toObjects(CustomerRequest::class.java)
                    val sortedRequests = requests.sortedByDescending { it.timestamp }
                    onUpdate(sortedRequests)
                }
            }
    }

    fun completeRequest(requestId: String, onSuccess: () -> Unit) {
        requestsCollection.document(requestId).update("status", "completed")
            .addOnSuccessListener { onSuccess() }
    }

    // YENİ: Yorumları getir
    fun getReviews(restaurantId: String, onSuccess: (List<Review>) -> Unit, onFailure: (Exception) -> Unit) {
        restaurantsCollection.document(restaurantId).get()
            .addOnSuccessListener { snapshot ->
                val restaurant = snapshot.toObject(Restaurant::class.java)
                onSuccess(restaurant?.reviews ?: emptyList())
            }
            .addOnFailureListener { onFailure(it) }
    }

    // YENİ: Yoruma cevap ver
    fun replyToReview(restaurantId: String, reviewId: String, reply: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        restaurantsCollection.document(restaurantId).get()
            .addOnSuccessListener { snapshot ->
                val restaurant = snapshot.toObject(Restaurant::class.java)
                if (restaurant != null) {
                    val updatedReviews = restaurant.reviews.map { review ->
                        if (review.id == reviewId) {
                            review.copy(ownerReply = reply)
                        } else {
                            review
                        }
                    }
                    restaurantsCollection.document(restaurantId).update("reviews", updatedReviews)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}