package com.ediapp.dhammapada.data

data class DhammapadaItem(
    val id: Long,
    val category: String,
    val title: String,
    val content: String,
    val myContent: String?,
    val createdAt: Long,
    val regDate: Long,
    val writeDate: Long,
    val url: String,
    val readCount: Int,
    val readTime: Long,
    val status: String,
    val accuracy: Double = 0.0,
    val bookmark: Int = 0
)
