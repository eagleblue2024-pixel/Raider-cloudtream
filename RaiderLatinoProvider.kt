package com.raiderlatino

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element

/**
 * Raider-Latino CloudStream 3 Provider
 * Películas en Español Latino Dual con resolución 4K UHD y 1080p.
 * Origen de datos completamente anonimizado y protegido.
 */
class RaiderLatinoProvider : MainAPI() {
    override var mainUrl = "https://www.cinecalidad.my"
    override var name = "Raider-Latino"
    override val hasMainPage = true
    override var lang = "es"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "" to "🔥 Estrenos & Tendencias Raider",
        "peliculas/4k-ultra-hd" to "✨ Películas 4K Ultra HD",
        "genero-peliculas/accion" to "💥 Acción",
        "genero-peliculas/ciencia-ficcion" to "🚀 Ciencia Ficción",
        "genero-peliculas/comedia" to "🍿 Comedia",
        "genero-peliculas/terror" to "👻 Terror",
        "genero-peliculas/suspenso" to "🕵️ Suspenso",
        "genero-peliculas/animacion" to "🎨 Animación",
        "genero-peliculas/drama" to "🎭 Drama",
        "genero-peliculas/crimen" to "🔪 Crimen",
        "genero-peliculas/romance" to "💖 Romance",
        "genero-peliculas/aventura" to "🗺️ Aventura",
        "genero-peliculas/fantasia" to "🧙 Fantasía",
        "genero-peliculas/misterio" to "🔍 Misterio",
        "peliculas/2026" to "📅 Estrenos 2026",
        "peliculas/2025" to "📅 Lo Mejor de 2025"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            if (request.data.isEmpty()) mainUrl else "$mainUrl/${request.data}/"
        } else {
            if (request.data.isEmpty()) "$mainUrl/page/$page/" else "$mainUrl/${request.data}/page/$page/"
        }

        val doc = app.get(
            url,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "Referer" to mainUrl
            )
        ).document

        val items = doc.select(".home_post_cont, .post_box").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val link = this.selectFirst("a") ?: return null
        val href = fixUrl(link.attr("href"))
        val img = this.selectFirst("img")
        val rawTitle = img?.attr("title")?.ifEmpty { null } ?: img?.attr("alt") ?: return null
        val title = rawTitle
            .replace(Regex("\\(\\d{4}\\).*"), "")
            .replace(Regex("(?i)cinecalidad|descargar|online|gratis|hd"), "")
            .trim()
        val year = Regex("\\((\\d{4})\\)").find(rawTitle)?.groupValues?.get(1)?.toIntOrNull()
        val posterUrl = img.attr("src").ifEmpty { null } ?: img.attr("data-src")
        val is4k = href.contains("4k") || rawTitle.contains("4k", ignoreCase = true)
        val quality = if (is4k) SearchQuality.UHD else SearchQuality.HD

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.year = year
            this.quality = quality
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cleanQuery = query.trim().replace(" ", "+")
        val url = "$mainUrl/?s=$cleanQuery"
        val doc = app.get(url).document

        return doc.select(".home_post_cont, .post_box").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val titleRaw = doc.selectFirst("h1")?.text()?.trim() ?: return null
        val title = titleRaw
            .replace(Regex("\\(\\d{4}\\).*"), "")
            .replace(Regex("(?i)cinecalidad|descargar|online|gratis|hd"), "")
            .trim()
        val year = Regex("\\((\\d{4})\\)").find(titleRaw)?.groupValues?.get(1)?.toIntOrNull()
        val poster = doc.selectFirst(".single_left img")?.attr("src")
        val plot = doc.selectFirst(".single_left p")?.text()?.trim()
            ?.replace(Regex("(?i)cinecalidad"), "Raider-Latino")
        val rating = doc.selectFirst(".imdb-box, a[href*='imdb.com']")?.text()?.let {
            Regex("(\\d+(\\.\\d+)?)\\s*/\\s*10").find(it)?.groupValues?.get(1)
        }
        val genres = doc.select(".home_post_cat a, a[href*='genero-peliculas']").map { it.text().trim() }.distinct()
        val actors = doc.select("a[href*='/peliculas/']").map { it.text().trim() }
            .filter { !it.contains("4K", ignoreCase = true) && !Regex("^\\d{4}$").matches(it) && it.length > 2 }.distinct()
        val trailerCode = doc.selectFirst("#panel_online a[service='Trailer']")?.attr("data")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = plot
            this.tags = genres
            this.actors = actors
            this.rating = rating?.toIntOrNull()
            if (!trailerCode.isNullOrEmpty()) {
                this.addTrailer("https://www.youtube.com/watch?v=$trailerCode")
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        doc.select("#panel_online a.onlinelink").forEach { link ->
            val service = link.attr("service")
            val id = link.attr("data")
            if (id.isEmpty()) return@forEach

            when (service) {
                "OnlineFilemoon" -> {
                    loadExtractor("https://filemoon.sx/e/$id", subtitleCallback, callback)
                    loadExtractor("https://bysedikamoum.com/e/$id", subtitleCallback, callback)
                }
                "OnlineVoe" -> {
                    loadExtractor("https://voe.sx/e/$id", subtitleCallback, callback)
                }
                "OnlineDoodstream" -> {
                    loadExtractor("https://doodstream.com/e/$id", subtitleCallback, callback)
                    loadExtractor("https://dood.to/e/$id", subtitleCallback, callback)
                }
                "OnlineMega" -> {
                    callback(
                        ExtractorLink(
                            source = "Raider-Mega",
                            name = "Raider-Latino (Mega)",
                            url = "https://mega.nz/embed/$id",
                            referer = "",
                            quality = Qualities.P1080.value
                        )
                    )
                }
                "OnlineNetu" -> {
                    loadExtractor("https://www.zarato.top/watch_video.php?v=$id", subtitleCallback, callback)
                }
            }
        }
        return true
    }
}
