package com.example.meteost3f.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReverseGeocodingResponse(
    val address: NominatimAddress? = null,
)

@Serializable
data class NominatimAddress(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    val country: String? = null,
)
