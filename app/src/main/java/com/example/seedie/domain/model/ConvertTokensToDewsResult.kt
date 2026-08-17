package com.example.seedie.domain.model

enum class ConvertTokensToDewsResult {
    Success,
    NotEnoughTokens,
    DailyConvertCapReached,
    DailyDewCapReached,
    NotLoggedIn,
    InvalidAmount
}
