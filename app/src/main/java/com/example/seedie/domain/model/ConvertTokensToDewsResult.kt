package com.example.seedie.domain.model

enum class ConvertTokensToDewsResult {
    Success,
    NotEnoughTokens,
    DailyConvertCapReached,
    NotLoggedIn,
    InvalidAmount
}
