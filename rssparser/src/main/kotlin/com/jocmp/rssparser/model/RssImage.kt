package com.jocmp.rssparser.model

data class RssImage(
    val title: String? = null,
    val url: String?,
    val link: String? = null,
    val description: String? = null
) {
    fun isNotEmpty(): Boolean {
        return !url.isNullOrBlank() || !link.isNullOrBlank()
    }

    internal data class Builder(
        private var title: String? = null,
        private var url: String? = null,
        private var link: String? = null,
        private var description: String? = null
    ) {
        fun title(title: String?) = apply { this.title = title }
        fun url(url: String?) = apply { this.url = url }
        fun link(link: String?) = apply { this.link = link }
        fun description(description: String?) = apply { this.description = description }
        fun build() = RssImage(title, url, link, description)
    }
}
