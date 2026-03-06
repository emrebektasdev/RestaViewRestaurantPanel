package com.ebco.restaviewrestorangirisi

data class Restaurant(
    val id: String = "", 
    val ownerId: String = "", 
    val name: String = "",
    val description: String = "",
    val city: String = "",
    val address: String = "",
    val tableCount: Int = 10, 
    val hygieneScore: Double = 0.0,
    val tasteScore: Double = 0.0,
    val priceScore: Double = 0.0,
    val rwScore: Double = 0.0,
    val imageResId: Int = 0, 
    val imageUrl: String = "", 
    val reviews: List<Review> = emptyList()
)

data class Review(
    val id: String = "", 
    val restaurantId: String = "",
    val userId: String = "", // YENİ
    val userName: String = "",
    val hygieneRating: Int = 0,
    val tasteRating: Int = 0,
    val priceRating: Int = 0,
    val comment: String = "",
    val isPositive: Boolean = false,
    val date: String = "",
    val ownerReply: String = "" 
)

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val userType: String = "owner" 
)

data class CustomerRequest(
    val id: String = "",
    val restaurantId: String = "",
    val tableName: String = "",
    val requestType: String = "", 
    val note: String = "", 
    val status: String = "pending", 
    val timestamp: Long = 0
)