package com.intent.reader.data.assets

/**
 * Built-in recommended sources (doc 4.2.2). Static list, ~30 entries with
 * name / feed URL / logo URL / category. Logos use site apple-touch-icons.
 */
data class RecommendedSource(
    val name: String,
    val feedUrl: String,
    val logoUrl: String,
    val category: String,
)

object RecommendedSources {

    val all: List<RecommendedSource> = listOf(
        // --- News (EN) ---
        RecommendedSource(
            "BBC News", "https://feeds.bbci.co.uk/news/rss.xml",
            "https://www.bbc.co.uk/apple-touch-icon.png", "News"
        ),
        RecommendedSource(
            "BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml",
            "https://www.bbc.co.uk/apple-touch-icon.png", "News"
        ),
        RecommendedSource(
            "The Guardian", "https://www.theguardian.com/world/rss",
            "https://www.theguardian.com/apple-touch-icon.png", "News"
        ),
        RecommendedSource(
            "The New York Times", "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
            "https://www.nytimes.com/apple-touch-icon.png", "News"
        ),
        RecommendedSource(
            "CNN", "http://rss.cnn.com/rss/edition.rss",
            "https://www.cnn.com/apple-touch-icon.png", "News"
        ),
        RecommendedSource(
            "NPR News", "https://feeds.npr.org/1001/rss.xml",
            "https://www.npr.org/apple-touch-icon-precomposed.png", "News"
        ),
        RecommendedSource(
            "Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml",
            "https://www.aljazeera.com/apple-touch-icon.png", "News"
        ),
        RecommendedSource(
            "NASA Breaking News", "https://www.nasa.gov/feed/",
            "https://www.nasa.gov/apple-touch-icon.png", "Science"
        ),
        RecommendedSource(
            "Science Daily", "https://www.sciencedaily.com/rss/top/science.xml",
            "https://www.sciencedaily.com/apple-touch-icon.png", "Science"
        ),
        RecommendedSource(
            "ESPN Top News", "https://www.espn.com/espn/rss/news",
            "https://www.espn.com/apple-touch-icon.png", "Sports"
        ),
        // --- Tech (EN) ---
        RecommendedSource(
            "The Verge", "https://www.theverge.com/rss/index.xml",
            "https://www.theverge.com/apple-touch-icon.png", "Tech"
        ),
        RecommendedSource(
            "TechCrunch", "https://techcrunch.com/feed/",
            "https://techcrunch.com/wp-content/uploads/2015/02/cropped-cropped-favicon-gradient.png", "Tech"
        ),
        RecommendedSource(
            "Hacker News", "https://hnrss.org/frontpage",
            "https://news.ycombinator.com/favicon.ico", "Tech"
        ),
        RecommendedSource(
            "Ars Technica", "https://feeds.arstechnica.com/arstechnica/index",
            "https://arstechnica.com/favicon.ico", "Tech"
        ),
        RecommendedSource(
            "Wired", "https://www.wired.com/feed/rss",
            "https://www.wired.com/favicon.ico", "Tech"
        ),
        RecommendedSource(
            "Engadget", "https://www.engadget.com/rss.xml",
            "https://www.engadget.com/favicon.ico", "Tech"
        ),
        RecommendedSource(
            "9to5Mac", "https://9to5mac.com/feed/",
            "https://9to5mac.com/favicon.ico", "Tech"
        ),
        RecommendedSource(
            "MacRumors", "https://feeds.macrumors.com/MacRumors-All",
            "https://www.macrumors.com/favicon.ico", "Tech"
        ),
        // --- Design ---
        RecommendedSource(
            "Smashing Magazine", "https://www.smashingmagazine.com/feed/",
            "https://www.smashingmagazine.com/favicon.ico", "Design"
        ),
        RecommendedSource(
            "Design Milk", "https://design-milk.com/feed/",
            "https://design-milk.com/favicon.ico", "Design"
        ),
        // --- Chinese ---
        RecommendedSource(
            "36氪", "https://36kr.com/feed",
            "https://36kr.com/favicon.ico", "中文"
        ),
        RecommendedSource(
            "少数派", "https://sspai.com/feed",
            "https://cdn-static.sspai.com/favicon/sspai.ico", "中文"
        ),
        RecommendedSource(
            "虎嗅", "https://www.huxiu.com/rss/0.xml",
            "https://www.huxiu.com/favicon.ico", "中文"
        ),
        RecommendedSource(
            "爱范儿", "https://www.ifanr.com/feed",
            "https://www.ifanr.com/favicon.ico", "中文"
        ),
        RecommendedSource(
            "阮一峰的网络日志", "https://www.ruanyifeng.com/blog/atom.xml",
            "https://www.ruanyifeng.com/favicon.ico", "中文"
        ),
        RecommendedSource(
            "知乎日报", "https://news-at.zhihu.com/api/3/news/latest", // placeholder API, kept for future
            "https://static.zhihu.com/heifetz/favicon.ico", "中文"
        ),
        RecommendedSource(
            "V2EX", "https://www.v2ex.com/index.xml",
            "https://www.v2ex.com/favicon.ico", "中文"
        ),
        // --- Japanese ---
        RecommendedSource(
            "NHK ニュース", "https://www3.nhk.or.jp/rss/news/cat0.xml",
            "https://www3.nhk.or.jp/favicon.ico", "日本語"
        ),
        RecommendedSource(
            "Yahoo!ニュース 主要", "https://news.yahoo.co.jp/pickup/domestic/rss.xml",
            "https://s.yimg.jp/images/favicon.ico", "日本語"
        ),
        RecommendedSource(
            "ITmedia NEWS", "https://rss.itmedia.co.jp/rss/2.0/news.xml",
            "https://www.itmedia.co.jp/favicon.ico", "日本語"
        ),
        // --- Korean ---
        RecommendedSource(
            "연합뉴스 속보", "https://www.yonhapnewstv.co.kr/rss/news/1",
            "https://www.yonhapnewstv.co.kr/favicon.ico", "한국어"
        ),
    )

    fun byCategory(): Map<String, List<RecommendedSource>> = all.groupBy { it.category }
}
