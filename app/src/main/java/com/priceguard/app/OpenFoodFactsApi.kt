package com.priceguard.app

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// Response structure matching Open Food Facts API
data class OffProductResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "product") val product: OffProduct?
)

data class OffProduct(
    @Json(name = "product_name") val productName: String?,
    @Json(name = "brands") val brands: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "nutriscore_grade") val nutriscoreGrade: String?,
    @Json(name = "categories") val categories: String?,
    @Json(name = "nova_group") val novaGroup: Int?,
    @Json(name = "additives_n") val additivesCount: Int?
)

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): Response<OffProductResponse>

    companion object {
        private val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        fun create(): OpenFoodFactsApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            return retrofit.create(OpenFoodFactsApi::class.java)
        }
    }
}
