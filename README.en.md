<p align="center">
  <img src="assets/google-play/icon-512.png" width="112" alt="INTENT icon" />
</p>

<h1 align="center">INTENT User Guide</h1>

<p align="center">
  <a href="README.md">简体中文</a> | <b>English</b><br />
  <b>INTENT — an RSS reader designed for focused reading, with lightweight AI features</b>
</p>

<p align="center">
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/kotlin-2.4.21-7F52FF" alt="Kotlin" /></a>
  <a href="https://kotlinlang.org/compose-multiplatform/"><img src="https://img.shields.io/badge/compose-1.12.2-4285F4" alt="Compose Multiplatform" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT" /></a>
</p>

<p align="center">
  <img width="4096" height="1920" alt="INTENT app overview" src="https://github.com/user-attachments/assets/b9b0ecbc-7cb1-4680-8265-5a560808230c" />
</p>

<p align="center">
  For INTENT v1.6 and later · Android 11 or later
</p>

<p align="center">
  <a href="https://intentread.pages.dev/#features">Website</a> · <a href="https://intentsonnet.pages.dev/help.html">Help Center</a> · <a href="CHANGELOG.md">Changelog</a> · <a href="https://github.com/EOVANS/INTENT/releases">Releases</a>
</p>

<p align="center">
  <a href="https://github.com/EOVANS/INTENT/releases/download/v1.7.3/INTENT.1.7.3.apk">Download INTENT v1.7.3</a> · <a href="https://gh.absinthe.life/github.com/EOVANS/INTENT/releases/download/v1.7.3/INTENT.1.7.3.apk">Accelerated download for mainland China</a>
</p>

## Contents

1. [Getting to know INTENT](#1-getting-to-know-intent)
2. [Adding subscriptions](#2-adding-subscriptions)
3. [Finding RSS feeds](#3-finding-rss-feeds)
4. [Recommended feeds](#4-recommended-feeds)
5. [Following social media and newsletters](#5-following-social-media-and-newsletters)
6. [Managing subscriptions](#6-managing-subscriptions)
7. [Reading and notes](#7-reading-and-notes)
8. [Home screen widgets](#8-home-screen-widgets)
9. [Personalization and settings](#9-personalization-and-settings)
10. [Frequently asked questions](#10-frequently-asked-questions)

## 1. Getting to know INTENT

RSS lets you follow websites, publications, and blogs in one place. When a source publishes an update, INTENT collects it for you.

The bottom navigation has three destinations:

- **Home:** a daily quote and article cards from your subscriptions. Each source shows a preview of five articles; select **View all** to see more.
- **Notes:** your weekly note count and saved excerpts, grouped by article.
- **Settings:** appearance, accessibility, notifications, and more.

On first launch, you can enter a name for the Home greeting and choose whether to enable notifications. Both can be changed later in Settings.

## 2. Adding subscriptions

Tap the round search button at the top of Home. You can add sources in three ways:

1. **Recommended sources:** browse the featured carousel, tap a logo, and select **Subscribe**.
2. **Search or paste a link:** enter a website name or an RSS URL. If you paste a regular webpage URL, INTENT will try to discover its feed.
3. **Import OPML:** export an OPML file from another reader, copy it to your device, and use the OPML import option at the bottom of Settings. You can also export your subscriptions for backup or migration.

## 3. Finding RSS feeds

Some websites advertise an RSS link in their footer or page source. You can also try common paths after the website address, such as `/feed`, `/rss`, `/atom.xml`, `/feed.xml`, `/rss.xml`, or `/index.xml`.

To inspect a page's source, search for `rss`, `atom`, or `xml` and look for a link like:

```html
<link rel="alternate" type="application/rss+xml" href="...">
```

Copy the URL in `href` into INTENT. Not every website offers a feed, and feed addresses may change.

For a topic-based feed, Google News supports search URLs such as:

```text
https://news.google.com/rss/search?q=artificial%20intelligence&hl=en-US&gl=US&ceid=US:en
```

You can also use [RSSHub](https://docs.rsshub.app/) to generate feeds for supported platforms. Public instances can be unreliable; see its documentation for routes and self-hosting options.

## 4. Recommended feeds

Paste a feed URL into INTENT's search page. Availability depends on the publisher and your network.

| Category | Source | Feed URL |
| --- | --- | --- |
| INTENT | INTENT Devblog | `https://intentblog.pages.dev/feed.xml` |
| News | BBC News | `https://feeds.bbci.co.uk/news/rss.xml` |
| News | The Guardian: World | `https://www.theguardian.com/world/rss` |
| News | The New York Times | `https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml` |
| News | NPR | `https://feeds.npr.org/1001/rss.xml` |
| News | Al Jazeera | `https://www.aljazeera.com/xml/rss/all.xml` |
| Technology | The Verge | `https://www.theverge.com/rss/index.xml` |
| Technology | TechCrunch | `https://techcrunch.com/feed/` |
| Technology | Ars Technica | `https://feeds.arstechnica.com/arstechnica/index` |
| Technology | Hacker News | `https://hnrss.org/frontpage` |
| Development | Android Developers Blog | `https://android-developers.googleblog.com/feeds/posts/default` |
| Development | Kotlin Blog | `https://blog.jetbrains.com/kotlin/feed/` |
| Development | GitHub Blog | `https://github.blog/feed/` |
| Science | Nature | `https://www.nature.com/nature.rss` |
| Science | Science News | `https://www.science.org/rss/news_current.xml` |
| Health | STAT | `https://www.statnews.com/feed/` |
| Business | CNBC | `https://www.cnbc.com/id/100003114/device/rss/rss.html` |
| Culture | Longreads | `https://longreads.com/feed/` |
| Culture | The Marginalian | `https://www.themarginalian.org/feed/` |
| History | JSTOR Daily | `https://daily.jstor.org/feed/` |

For a larger directory, including Chinese, Japanese, Korean, medical, legal, and arts sources, see the [feed list in the Chinese guide](README.md#4-常见-rss-源推荐). You can also explore INTENT's built-in recommendations or import an OPML collection.

## 5. Following social media and newsletters

- **WeChat official accounts:** WeChat does not provide an official RSS feed. A self-hosted tool such as [WeWe RSS](https://github.com/cooderl/wewe-rss) can generate feeds using WeChat Reading. Consult its documentation for setup and limitations.
- **Weibo, Bilibili, Zhihu, Douban, and other platforms:** check [RSSHub's documentation](https://docs.rsshub.app/) for available routes.
- **Email newsletters:** services such as Kill the Newsletter can turn email subscriptions into RSS feeds.

## 6. Managing subscriptions

Open the subscribed RSS list from the search page:

- Tap a source to see its articles.
- Long-press and drag to reorder sources.
- Swipe left to unsubscribe.
- Open an individual source's settings to change its display name, sync frequency, or notifications.
- Open the all-articles view to filter by source, date, read status, or keywords.

## 7. Reading and notes

Pull down on Home to refresh subscriptions. Tap a card to read an article, or long-press it for actions such as changing its read status or sharing it. The reader keeps navigation, notes, AI Glance, and text-to-speech controls available at the top. Article headings are visually distinguished from body text when the feed supplies heading markup.

**AI Glance** generates a summary using your configured AI provider. Results are cached locally and reused until you request a new summary. Configure an engine in accessibility settings to use text-to-speech. To reduce data use, you can restrict image loading to Wi-Fi.

### Island Notes

- Long-press an article paragraph to highlight it and optionally add a remark. Tap a highlighted paragraph to edit or delete the note.
- The **Notes** tab groups saved paragraphs by article. Use its search button to find notes, or the reader's bookmark button to view notes for the current article.
- Notes are stored locally as Markdown, with the article title, source, link, excerpt, and remark. They do not require an AI provider.
- Clearing the ordinary cache, cleaning up old articles, or unsubscribing does not delete notes. Share a Markdown copy from the Notes screen before uninstalling the app or clearing its data.

## 8. Home screen widgets

INTENT provides two Material 3 widgets:

- **4 × 4:** a list of today's articles.
- **4 × 2:** a horizontal widget for browsing article titles.

To add one, long-press an empty area on your home screen, open the widget picker, find **INTENT**, and drag the widget onto the screen. Tap an article in a widget to open it. Widgets follow the app's accent color and light or dark theme; unavailable images use a placeholder.

## 9. Personalization and settings

- **AI settings:** choose a provider preset or configure a custom endpoint, enter a model ID and API key, enable the feature, and save. Connection tests may consume a small amount of your API allowance. Changing the provider or domain clears the previous key.
- **AI quotes:** optionally enable this separately. When candidate article text is available, the app can try to select and verify a quote while Home is in the foreground. An offline quote remains available if the request fails.
- **Appearance:** change the app icon, font, accent color, and light or dark theme. The Home quote keeps its serif font. Available interface languages include Simplified Chinese, English, Japanese, and Korean.
- **Accessibility:** configure text-to-speech, display and text sizes, contrast, touch targets, and animations.
- **Notifications:** configure the daily digest and its delivery time. Reading progress as a live activity requires Android 16; the control is unavailable on older versions.

## 10. Frequently asked questions

**Why are articles not updating?**  
Pull down to refresh, check your connection, and review the source's sync frequency. Some publishers update infrequently.

**Why can't INTENT find a feed?**  
Check the URL using the methods above, or paste the website address to try automatic discovery. A public RSSHub instance may be temporarily unavailable.

**Why are images missing?**  
Check whether image loading is restricted to Wi-Fi. An image may also be unavailable at its source.

**Is my data uploaded?**  
AI is off by default. When you configure a provider and use an AI feature, the relevant article text is sent to that provider and its API charges apply. API keys are encrypted locally; AI settings and result caches are excluded from Android system backup. Other local data follows the device's backup settings.

**What are the limits of AI features?**  
They use text already obtained by INTENT; they do not analyze images or fetch additional webpages. If only an RSS excerpt is available, the app indicates that scope. Articles over 24,000 characters are not sent. Provider errors or insufficient credit are reported without repeated automatic retries.

**How do I move to a new phone?**  
Export your subscriptions as OPML and import the file on the new device. Export or share your notes separately before removing the app or clearing its data.

**Why didn't the widget color change?**  
Widgets normally refresh after an accent-color change. If your launcher has cached the old appearance, remove and add the widget again.

---

*Enjoy focused reading with INTENT.*

## AI tools used by this project

![Claude Code](https://img.shields.io/badge/Claude%20Code-%23D97757.svg?style=for-the-badge&logo=claudecode&logoColor=white)
![DeepSeek](https://img.shields.io/badge/DeepSeek-%235786FE.svg?style=for-the-badge&logo=deepseek&logoColor=white)
![Google Gemini](https://img.shields.io/badge/google%20gemini-%238E75B2.svg?style=for-the-badge&logo=google%20gemini&logoColor=white)
![Minimax](https://img.shields.io/badge/minimax-%23B4393C.svg?style=for-the-badge&logo=minimax&logoColor=white)
![Ollama](https://img.shields.io/badge/ollama-%23000000.svg?style=for-the-badge&logo=ollama&logoColor=white)

## License

Original project code is licensed under the [MIT License](LICENSE), copyright Cynosure/Atelier. Third-party components are subject to their own licenses; for example, Android Liquid Glass uses the [Apache License 2.0](third-party/android-liquid-glass/LICENSE).

## Star History

<a href="https://star-history.com/#EOVANS/INTENT&amp;Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=EOVANS/INTENT&amp;type=Date&amp;theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=EOVANS/INTENT&amp;type=Date" />
    <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=EOVANS/INTENT&amp;type=Date" />
  </picture>
</a>
