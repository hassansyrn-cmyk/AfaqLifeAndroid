package com.uae.goldprice

import com.google.gson.annotations.SerializedName

data class GoldResponse(
    @SerializedName("price") val price: Double,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("currency") val currency: String
)

data class GoldPriceModel(
    val karat24: Double,
    val karat22: Double,
    val karat21: Double,
    val karat18: Double,
    val updatedAt: String
)
