package com.intent.reader.data.assets

/**
 * Built-in recommended sources (doc 4.2.2). Static list grouped into the six
 * browse categories shown in the search page's hero strip. Category keys map to
 * the localized labels in strings.xml (category_news, category_tech, …).
 */
data class RecommendedSource(
    val name: String,
    val feedUrl: String,
    val logoUrl: String,
    val category: String,
)

object RecommendedSources {

    const val CATEGORY_NEWS = "news"
    const val CATEGORY_TECH = "tech"
    const val CATEGORY_MEDICINE = "medicine"
    const val CATEGORY_LAW = "law"
    const val CATEGORY_HISTORY = "history"
    const val CATEGORY_CULTURE = "culture"

    /** Hero strip order. */
    val categories: List<String> = listOf(
        CATEGORY_NEWS,
        CATEGORY_TECH,
        CATEGORY_MEDICINE,
        CATEGORY_LAW,
        CATEGORY_HISTORY,
        CATEGORY_CULTURE,
    )

    val all: List<RecommendedSource> = listOf(
        // ---------- 时事 ----------
        RecommendedSource(
            "BBC News", "https://feeds.bbci.co.uk/news/rss.xml",
            "https://www.bbc.co.uk/apple-touch-icon.png", CATEGORY_NEWS
        ),
        RecommendedSource(
            "BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml",
            "https://www.bbc.co.uk/apple-touch-icon.png", CATEGORY_NEWS
        ),
        RecommendedSource(
            "The Guardian", "https://www.theguardian.com/world/rss",
            "https://www.theguardian.com/apple-touch-icon.png", CATEGORY_NEWS
        ),
        RecommendedSource(
            "The New York Times", "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
            "https://www.nytimes.com/apple-touch-icon.png", CATEGORY_NEWS
        ),
        RecommendedSource(
            "Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml",
            "https://www.aljazeera.com/apple-touch-icon.png", CATEGORY_NEWS
        ),
        RecommendedSource(
            "NPR News", "https://feeds.npr.org/1001/rss.xml",
            "https://www.npr.org/apple-touch-icon-precomposed.png", CATEGORY_NEWS
        ),
        RecommendedSource(
            "NHK ニュース", "https://www.nhk.or.jp/rss/news/cat0.xml",
            "https://www3.nhk.or.jp/favicon.ico", CATEGORY_NEWS
        ),
        RecommendedSource(
            "The Conversation", "https://theconversation.com/uk/articles.atom",
            "https://theconversation.com/favicon.ico", CATEGORY_NEWS
        ),
        RecommendedSource(
            "DW", "https://rss.dw.com/rdf/rss-en-all",
            "https://www.dw.com/favicon.ico", CATEGORY_NEWS
        ),
        RecommendedSource(
            "France 24", "https://www.france24.com/en/rss",
            "https://www.france24.com/favicon.ico", CATEGORY_NEWS
        ),
        RecommendedSource(
            "CNBC", "https://www.cnbc.com/id/100003114/device/rss/rss.html",
            "https://www.cnbc.com/favicon.ico", CATEGORY_NEWS
        ),
        RecommendedSource(
            "朝日新聞", "https://www.asahi.com/rss/asahi/newsheadlines.rdf",
            "https://www.asahi.com/favicon.ico", CATEGORY_NEWS
        ),
        RecommendedSource(
            "中央社 CNA", "https://feeds.feedburner.com/rsscna/intworld",
            "https://www.cna.com.tw/favicon.ico", CATEGORY_NEWS
        ),

        // ---------- 科技 ----------
        RecommendedSource(
            "The Verge", "https://www.theverge.com/rss/index.xml",
            "https://www.theverge.com/apple-touch-icon.png", CATEGORY_TECH
        ),
        RecommendedSource(
            "TechCrunch", "https://techcrunch.com/feed/",
            "https://techcrunch.com/wp-content/uploads/2015/02/cropped-cropped-favicon-gradient.png",
            CATEGORY_TECH
        ),
        RecommendedSource(
            "Hacker News", "https://hnrss.org/frontpage",
            "https://news.ycombinator.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "Ars Technica", "https://feeds.arstechnica.com/arstechnica/index",
            "https://arstechnica.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "Wired", "https://www.wired.com/feed/rss",
            "https://www.wired.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "Engadget", "https://www.engadget.com/rss.xml",
            "https://www.engadget.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "9to5Mac", "https://9to5mac.com/feed/",
            "https://9to5mac.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "MacRumors", "https://feeds.macrumors.com/MacRumors-All",
            "https://www.macrumors.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "MIT Technology Review", "https://www.technologyreview.com/feed/",
            "https://www.technologyreview.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "IEEE Spectrum", "https://spectrum.ieee.org/feeds/feed.rss",
            "https://spectrum.ieee.org/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "The Register", "https://www.theregister.com/headlines.atom",
            "https://www.theregister.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "Android Authority", "https://www.androidauthority.com/feed/",
            "https://www.androidauthority.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "Tom's Hardware", "https://www.tomshardware.com/feeds/all",
            "https://www.tomshardware.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "InfoQ 中文", "https://www.infoq.cn/feed",
            "https://www.infoq.cn/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "少数派", "https://sspai.com/feed",
            "https://cdn-static.sspai.com/favicon/sspai.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "爱范儿", "https://www.ifanr.com/feed",
            "https://www.ifanr.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "ITmedia NEWS", "https://rss.itmedia.co.jp/rss/2.0/news_bursts.xml",
            "https://www.itmedia.co.jp/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "V2EX", "https://www.v2ex.com/index.xml",
            "https://www.v2ex.com/favicon.ico", CATEGORY_TECH
        ),
        RecommendedSource(
            "阮一峰的网络日志", "https://www.ruanyifeng.com/blog/atom.xml",
            "https://www.ruanyifeng.com/favicon.ico", CATEGORY_TECH
        ),

        // ---------- 医学 ----------
        RecommendedSource(
            "Nature Medicine", "https://www.nature.com/nm.rss",
            "https://www.nature.com/apple-touch-icon.png", CATEGORY_MEDICINE
        ),
        RecommendedSource(
            "STAT", "https://www.statnews.com/feed/",
            "https://www.statnews.com/apple-touch-icon.png", CATEGORY_MEDICINE
        ),
        RecommendedSource(
            "The Lancet", "https://www.thelancet.com/rssfeed/lancet_current.xml",
            "https://www.thelancet.com/favicon.ico", CATEGORY_MEDICINE
        ),
        RecommendedSource(
            "Healthline", "https://www.healthline.com/rss/health-news",
            "https://www.healthline.com/favicon.ico", CATEGORY_MEDICINE
        ),
        RecommendedSource(
            "Medical Xpress", "https://medicalxpress.com/rss-feed/",
            "https://medicalxpress.com/favicon.ico", CATEGORY_MEDICINE
        ),
        RecommendedSource(
            "MedPage Today", "https://www.medpagetoday.com/rss/headlines.xml",
            "https://www.medpagetoday.com/apple-touch-icon.png", CATEGORY_MEDICINE
        ),
        RecommendedSource(
            "WHO News", "https://www.who.int/rss-feeds/news-english.xml",
            "https://www.who.int/apple-touch-icon.png", CATEGORY_MEDICINE
        ),
        RecommendedSource(
            "ScienceDaily Health", "https://www.sciencedaily.com/rss/health_medicine.xml",
            "https://www.sciencedaily.com/apple-touch-icon.png", CATEGORY_MEDICINE
        ),
        RecommendedSource(
            "NEJM", "https://www.nejm.org/action/showFeed?type=etoc&feed=rss&jc=nejm",
            "https://www.nejm.org/apple-touch-icon.png", CATEGORY_MEDICINE
        ),

        // ---------- 法学 ----------
        RecommendedSource(
            "SCOTUSblog", "https://www.scotusblog.com/feed/",
            "https://www.scotusblog.com/apple-touch-icon.png", CATEGORY_LAW
        ),
        RecommendedSource(
            "Above the Law", "https://abovethelaw.com/feed/",
            "https://abovethelaw.com/apple-touch-icon.png", CATEGORY_LAW
        ),
        RecommendedSource(
            "The Volokh Conspiracy", "https://reason.com/volokh/feed/",
            "https://reason.com/apple-touch-icon.png", CATEGORY_LAW
        ),
        RecommendedSource(
            "Lawfare", "https://www.lawfaremedia.org/feed",
            "https://www.lawfaremedia.org/apple-touch-icon.png", CATEGORY_LAW
        ),
        RecommendedSource(
            "JURIST", "https://www.jurist.org/feed/",
            "https://www.jurist.org/apple-touch-icon.png", CATEGORY_LAW
        ),
        RecommendedSource(
            "ABA Journal", "https://www.abajournal.com/rss/",
            "https://www.abajournal.com/favicon.ico", CATEGORY_LAW
        ),
        RecommendedSource(
            "Legal Cheek", "https://www.legalcheek.com/feed/",
            "https://www.legalcheek.com/favicon.ico", CATEGORY_LAW
        ),
        RecommendedSource(
            "EJIL:Talk!", "https://www.ejiltalk.org/feed/",
            "https://www.ejiltalk.org/favicon.ico", CATEGORY_LAW
        ),
        RecommendedSource(
            "Verfassungsblog", "https://verfassungsblog.de/feed/",
            "https://verfassungsblog.de/favicon.ico", CATEGORY_LAW
        ),

        // ---------- 历史 ----------
        RecommendedSource(
            "History Today", "https://www.historytoday.com/rss.xml",
            "https://www.historytoday.com/apple-touch-icon.png", CATEGORY_HISTORY
        ),
        RecommendedSource(
            "JSTOR Daily", "https://daily.jstor.org/feed/",
            "https://daily.jstor.org/apple-touch-icon.png", CATEGORY_HISTORY
        ),
        RecommendedSource(
            "Smithsonian History", "https://www.smithsonianmag.com/rss/history/",
            "https://www.smithsonianmag.com/apple-touch-icon.png", CATEGORY_HISTORY
        ),
        RecommendedSource(
            "History Extra", "https://www.historyextra.com/feed/",
            "https://www.historyextra.com/favicon.ico", CATEGORY_HISTORY
        ),
        RecommendedSource(
            "Lapham's Quarterly", "https://www.laphamsquarterly.org/rss.xml",
            "https://www.laphamsquarterly.org/favicon.ico", CATEGORY_HISTORY
        ),
        RecommendedSource(
            "Medievalists.net", "https://www.medievalists.net/feed/",
            "https://www.medievalists.net/apple-touch-icon.png", CATEGORY_HISTORY
        ),

        // ---------- 文化 ----------
        RecommendedSource(
            "The New Yorker", "https://www.newyorker.com/feed/everything",
            "https://www.newyorker.com/apple-touch-icon.png", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "Aeon", "https://aeon.co/feed.rss",
            "https://aeon.co/apple-touch-icon.png", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "Literary Hub", "https://lithub.com/feed/",
            "https://lithub.com/apple-touch-icon.png", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "The Paris Review", "https://www.theparisreview.org/blog/feed/",
            "https://www.theparisreview.org/apple-touch-icon.png", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "Design Milk", "https://design-milk.com/feed/",
            "https://design-milk.com/favicon.ico", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "The Atlantic", "https://www.theatlantic.com/feed/all/",
            "https://www.theatlantic.com/favicon.ico", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "Hyperallergic", "https://hyperallergic.com/feed/",
            "https://hyperallergic.com/favicon.ico", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "The Public Domain Review", "https://publicdomainreview.org/rss.xml",
            "https://publicdomainreview.org/favicon.ico", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "Pitchfork", "https://pitchfork.com/feed/feed-news/rss",
            "https://pitchfork.com/favicon.ico", CATEGORY_CULTURE
        ),
        RecommendedSource(
            "ARTnews", "https://www.artnews.com/feed/",
            "https://www.artnews.com/favicon.ico", CATEGORY_CULTURE
        ),
    )

    fun byCategory(key: String): List<RecommendedSource> = all.filter { it.category == key }

    /** Falls back to the full list when a route carries an unknown key. */
    fun byCategoryOrAll(key: String): List<RecommendedSource> =
        byCategory(key).ifEmpty { all }
}
