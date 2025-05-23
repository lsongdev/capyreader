package com.jocmp.readerclient

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagListResult(
    val tags: List<Tag>
)
