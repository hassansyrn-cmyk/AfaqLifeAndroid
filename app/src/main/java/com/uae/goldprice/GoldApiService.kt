package com.uae.goldprice

import retrofit2.http.GET

interface GoldApiService {
    @GET("price/XAU")
    suspend fun getGoldPrice(): GoldResponse
}
