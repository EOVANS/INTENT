# INTENT 用户使用手册

**INTENT — 一款专注于阅读的、由 AI 轻量赋能的 RSS 阅读器**

适用版本：Android 11 及以上
手册内容：添加订阅 · 找 RSS 源 · 管理与阅读 · 桌面小组件 · 个性化设置 · 常见问题

---

## 目录

1. [认识 INTENT](#1-认识-intent)
2. [添加订阅源](#2-添加订阅源)
3. [怎么找到一个网站的 RSS](#3-怎么找到一个网站的-rss)
4. [常见 RSS 源推荐](#4-常见-rss-源推荐)
5. [订阅微信公众号、微博、B站等](#5-订阅微信公众号微博b站等)
6. [管理你的订阅](#6-管理你的订阅)
7. [开始阅读](#7-开始阅读)
8. [桌面小组件](#8-桌面小组件)
9. [个性化设置](#9-个性化设置)
10. [常见问题](#10-常见问题)

---

## 1. 认识 INTENT

RSS 是一种"订阅"功能：你把喜欢的网站、媒体、博客添加进来，它们一更新，INTENT 就自动帮你收集，不需要逐个打开网站。

**三个底部标签页：**

- **首页**：顶部是当日金句（衬线字体，关键词红色下划线，点箭头回到原文），下方是今日文章卡片；
- **搜索**：搜索、添加和管理订阅源；
- **设置**：外观、无障碍、通知等。

**首次打开**：欢迎页可以填写你的称呼（显示在首页）、选择是否开启通知。可在设置里随时修改。

---

## 2. 添加订阅源

进入 **搜索** 页，有三种方式：

### 方式一：订阅推荐源（最简单）

1. 搜索框下方的 **Hero 横滑区** 里是推荐来源（BBC、The Guardian、CNN 等）；
2. 点击某个 logo，查看简介；
3. 点 **订阅**，按钮变为"已订阅"即完成。

### 方式二：搜索或粘贴链接

1. 顶部搜索框输入 RSS 地址（以 `http` 开头），或直接输入网站名称搜索；
2. 按提示确认订阅；
3. 如果你粘贴的是**普通网页地址**，INTENT 会自动尝试发现该网页的 RSS 地址。

### 方式三：批量导入（OPML）

OPML 是 RSS 订阅列表的通用格式，方便从其他阅读器迁移。

1. 先在原阅读器（如 Feedly、Inoreader、NewsBlur）里**导出 OPML 文件**；
2. 把文件放到手机中；
3. 在 **设置 →（底部）OPML 导入** 中选择该文件，所有订阅一键导入；
4. 同样可以 **导出 OPML**，用于备份或迁移。

---

## 3. 怎么找到一个网站的 RSS

大多数网站都提供 RSS，只是入口藏得深。按以下顺序试：

### 3.1 常见地址规律

在网站首页网址后依次尝试：

- `/feed`（最常见，如 `https://example.com/feed`）
- `/rss`
- `/atom.xml`、`/feed.xml`、`/rss.xml`、`/index.xml`

WordPress 搭建的网站、绝大多数博客，`/feed` 都有效。

### 3.2 看网页源码

1. 在浏览器打开该网站；
2. 查看网页源代码（电脑端右键"查看网页源代码"）；
3. 搜索 `rss`、`atom` 或 `xml`；
4. 找到形如
   `<link rel="alternate" type="application/rss+xml" href="...">`，
   `href` 里的地址就是 RSS 链接，复制到 INTENT 即可。

### 3.3 用 Google 新闻生成任意主题 RSS

想订阅某个话题（如"人工智能""护理考研"），即使没有网站也能用：

```
https://news.google.com/rss/search?q=关键词&hl=zh-CN&gl=CN&ceid=CN:zh-Hans
```

把"关键词"换成你想关注的词（中文可直接填，英文可改 `hl=en&gl=US`），粘贴到 INTENT 即可收到该话题的新闻。

### 3.4 RSSHub：万物皆可 RSS

很多没有官方 RSS 的平台（微博、B站、知乎、豆瓣、GitHub 等）都可以通过 **RSSHub** 生成：

- 文档：**docs.rsshub.app**（里面有每个平台的地址格式）；
- 公共实例：`https://rsshub.app/...`（不稳定时可按文档自建）；
- 例：B 站某 UP 主 `https://rsshub.app/bilibili/user/video/UP主UID`。

---

## 4. 常见 RSS 源推荐

> 地址以官方公布为准；复制到 INTENT 搜索框即可订阅。
> 找不到时用上方导航站或 Google 新闻 / RSSHub 自行生成。

### 4.1 综合·国际新闻

| 来源 | RSS 地址 |
|---|---|
| BBC News（英文） | `https://feeds.bbci.co.uk/news/rss.xml` |
| BBC 中文 | `https://feeds.bbci.co.uk/zhongwen/simp/rss.xml` |
| The Guardian（世界） | `https://www.theguardian.com/world/rss` |
| CNN（国际版） | `http://rss.cnn.com/rss/edition.rss` |
| New York Times | `https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml` |
| NPR | `https://feeds.npr.org/1001/rss.xml` |
| AP News | `https://feeds.apnews.com/apf-topnews` |
| Washington Post（世界） | `https://feeds.washingtonpost.com/rss/world` |
| Al Jazeera | `https://www.aljazeera.com/xml/rss/all.xml` |
| France 24（英文） | `https://www.france24.com/en/rss` |
| DW 德国之声（英文） | `https://rss.dw.com/rdf/rss-en-all` |
| Vox | `https://www.vox.com/rss/index.xml` |
| Politico | `https://www.politico.com/rss/politicopicks.xml` |
| 纽约时报中文网 | `https://cn.nytimes.com/rss/` |
| 德国之声中文 | `https://rss.dw.com/rdf/rss-chi-all` |
| 法国国际广播电台 RFI 中文 | `https://www.rfi.fr/cn/rss` |
| 美国之音中文 | `https://www.voachinese.com/api/zmgqeoim_` |

### 4.2 中文新闻

| 来源 | RSS 地址 |
|---|---|
| 联合早报（中国） | `https://www.zaobao.com.sg/rss/realtime/china` |
| 端传媒 | `https://theinitium.com/rss/news.xml` |
| 中央社（全部） | `https://www.cna.com.tw/rss/aall.xml` |
| 自由时报（全部） | `https://news.ltn.com.tw/rss/all.xml` |
| 风传媒 | `https://www.storm.mg/rss` |
| 日经中文网 | `https://cn.nikkei.com/feed/` |
| 新京报 / 澎湃 / 财新等 | 见导航站 quanwenrss.com（全文源） |

### 4.3 日本・韩语源

| 来源 | RSS 地址 |
|---|---|
| NHK 新闻（总合） | `https://www3.nhk.or.jp/rss/news/cat0.xml` |
| NHK 国际 | `https://www3.nhk.or.jp/rss/news/cat3.xml` |
| NHK WORLD（英文） | `https://www3.nhk.or.jp/nhkworld/en/news/feed.xml` |
| 朝日新闻（英文） | `https://www.asahi.com/ajw/rss/ajw-rss.xml` |
| 读卖 The Japan News | `https://japannews.yomiuri.co.jp/feed/` |
| 韩联社 Yonhap（头条） | `https://www.yna.co.kr/rss/news.xml` |
| 韩联社（朝鲜） | `https://www.yna.co.kr/rss/nk.xml` |
| 东亚日报 | `https://www.donga.com/news/rss/List_0213` |

### 4.4 科技·互联网

| 来源 | RSS 地址 |
|---|---|
| The Verge | `https://www.theverge.com/rss/index.xml` |
| TechCrunch | `https://techcrunch.com/feed/` |
| Ars Technica | `https://feeds.arstechnica.com/arstechnica/index` |
| Wired | `https://www.wired.com/feed/rss` |
| Engadget | `https://www.engadget.com/rss.xml` |
| Gizmodo | `https://gizmodo.com/rss` |
| Mashable | `https://mashable.com/feeds/rss/all` |
| Hacker News（头条） | `https://hnrss.org/frontpage` |
| MIT Technology Review | `https://www.technologyreview.com/feed/` |
| IEEE Spectrum | `https://spectrum.ieee.org/rss/fulltext` |
| Solidot（中文科技） | `https://www.solidot.org/index.rss` |
| 少数派 | `https://sspai.com/feed` |
| 36 氪 | `https://36kr.com/feed` |
| 虎嗅 | `https://www.huxiu.com/rss/0.xml` |
| 爱范儿 | `https://www.ifanr.com/feed` |
| 极客公园 | `https://www.geekpark.net/rss` |
| 量子位 | `https://www.qbitai.com/feed` |
| 机器之心 | `https://www.jiqizhixin.com/rss` |
| IT 之家 | `https://www.ithome.com/rss/` |
| cnBeta | `https://www.cnbeta.com.tw/backend.php` |
| 机核 | `https://www.gcores.com/rss` |

### 4.5 安卓·编程·开发

| 来源 | RSS 地址 |
|---|---|
| Android Developers Blog | `https://android-developers.googleblog.com/feeds/posts/default` |
| Kotlin Blog | `https://blog.jetbrains.com/kotlin/feed/` |
| Google Blog（AI） | `https://blog.google/technology/ai/rss/` |
| GitHub Blog | `https://github.blog/feed/` |
| Stack Overflow Blog | `https://stackoverflow.blog/feed/` |
| 9to5Google | `https://9to5google.com/feed/` |
| Android Police | `https://www.androidpolice.com/feed/` |
| Android Authority | `https://www.androidauthority.com/feed/` |
| InfoQ | `https://feed.infoq.com/` |
| Smashing Magazine | `https://www.smashingmagazine.com/feed/` |
| CSS-Tricks | `https://css-tricks.com/feed/` |
| A List Apart | `https://alistapart.com/feed/` |
| SitePoint | `https://www.sitepoint.com/feed/` |

### 4.6 学术·医学·护理

| 来源 | RSS 地址 |
|---|---|
| Nature（最新） | `https://www.nature.com/nature.rss` |
| Science（新闻） | `https://www.science.org/rss/news_current.xml` |
| NEJM 新英格兰医学杂志 | `https://www.nejm.org/action/showFeed?type=etoc&feed=rss&jc=nejm` |
| The Lancet | `https://www.thelancet.com/rssfeed/lancet_current.xml` |
| BMJ | `https://www.bmj.com/rss/current.xml` |
| PLOS Medicine | `https://journals.plos.org/plosmedicine/feed/atom` |
| PubMed 检索结果 | 在 PubMed 搜索结果页点"Create RSS"，订阅任意主题文献 |
| WHO 世界卫生组织（新闻） | `https://www.who.int/rss-feeds/news-english.xml` |
| CDC（新闻） | `https://tools.cdc.gov/api/v2/resources/media/403372.rss` |
| Medscape（护理） | `https://www.medscape.com/cx/rss/nursing.xml` |
| 医学/护理更多专题 | 用 PubMed RSS + Google 新闻生成（如关键词"nursing"） |

### 4.7 财经·商业

| 来源 | RSS 地址 |
|---|---|
| CNBC（头条） | `https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=100003114` |
| MarketWatch（头条） | `https://feeds.marketwatch.com/marketwatch/topstories/` |
| Forbes（商业） | `https://www.forbes.com/business/feed/` |
| Financial Times（前几篇） | `https://www.ft.com/rss/home` |
| 华尔街见闻 | 见 RSSHub / quanwenrss |

### 4.8 文化·长文·杂志

| 来源 | RSS 地址 |
|---|---|
| The Atlantic | `https://www.theatlantic.com/feed/all/` |
| The New Yorker | `https://www.newyorker.com/feed/rss` |
| Time | `https://time.com/feed/` |
| Longreads | `https://longreads.com/feed/` |
| The Marginalian（原 Brain Pickings） | `https://www.themarginalian.org/feed/` |
| AEON 思想长文 | `https://aeon.co/feed.rss` |
| Wait But Why | `https://waitbutwhy.com/feed` |

### 4.9 博客·社区

| 来源 | RSS 地址 |
|---|---|
| 阮一峰的网络日志 | `https://www.ruanyifeng.com/blog/atom.xml` |
| V2EX | `https://www.v2ex.com/index.xml` |
| 知乎每日精选 | `https://www.zhihu.com/rss` |
| 知乎日报（第三方） | `https://feedx.net/rss/zhihudaily.xml` |

### 4.10 更多源去哪找（导航站）

- **QuanwenRSS** `quanwenrss.com`：中文新闻全文源导航，336 个源，可直接下载 OPML；
- **BestBlogs**（ginonotes.com 合集）：1600+ 订阅源，含微信公众号源；
- **RSSHub 文档** `docs.rsshub.app`：社交媒体、平台类源；
- **RSS 源合集**：feedspot 分类榜单（如 `rss.feedspot.com/japanese_news_rss_feeds`）；
- 从导航站下载 OPML 后，按第 2 节"方式三"一键导入。

更多来源也可用 INTENT 搜索页的内置推荐列表。

---

## 5. 订阅微信公众号、微博、B站等

- **微信公众号**：微信没有官方 RSS。可自建开源工具 **WeWe RSS**（基于微信读书接口，用 Docker 部署，项目地址 `github.com/cooderl/wewe-rss`）：扫码登录微信读书 → 粘贴公众号文章链接 → 生成 RSS 地址，再复制到 INTENT 订阅。需要一台常开的服务器或 NAS；不建议短时间订阅大量公众号。
- **微博 / B站 / 知乎 / 豆瓣 / 抖音等**：按 [3.4](#34-rsshub万物皆可-rss) 使用 RSSHub 生成地址。
- **邮件通讯（Newsletter）**：可通过 RSS 聚合服务（如 Kill The Newsletter）把邮件订阅转成 RSS。

---

## 6. 管理你的订阅

进入 **搜索 → 已订阅的 RSS**：

- **查看**：点击某个来源，只看该来源的文章；
- **排序**：长按拖动调整顺序；
- **取消订阅**：左滑删除（仅删除订阅，不影响其他来源）；
- **重命名 / 单独设置**：在单个来源设置中可自定义显示名称、同步频率、是否开启通知；
- **全部文章**：搜索页点"全部文章"，可按来源、时间、已读状态筛选，支持关键词过滤。

---

## 7. 开始阅读

- **首页浏览**：卡片按时间排序，未读文章标题更粗，已读自动变淡；
- **下拉刷新**：手动更新所有订阅；
- **点击文章**：进入阅读页（干净的正文排版）；
- **长按卡片**：标记已读/未读、分享、取消订阅该来源；
- **金句区**：点末尾胶囊箭头直达金句出处；
- **朗读全文**：在无障碍设置中配置 TTS 引擎后，阅读页可朗读；
- **省流模式**：设置中可开启仅 Wi-Fi 加载图片，无图内容自动以无图卡片显示。

---

## 8. 桌面小组件

INTENT 提供两种 Material 3 桌面小组件：

- **4×4**：完整列表，显示今日多篇文章；
- **4×2**：横长条，横向滚动显示标题。

**添加方法**：长按手机桌面空白处 → 选择"小组件/微件" → 找到 **INTENT** → 拖动两种尺寸到桌面。

- 点击小组件文章直接进入阅读；
- 小组件颜色会跟随你在 App 内设置的**强调色和深浅主题**自动变化。

---

## 9. 个性化设置

进入 **设置**：

- **更改用户界面**：
  - 更换 App 图标；
  - 更换字体（首页顶部金句区固定为衬线，不会被更改）；
  - 更改强调色（默认红色，会联动小组件、开关、下划线等）；
  - 切换跟随系统 / 浅色 / 深色；
  - 切换语言（简体中文、English、日本語、한국어）；
- **无障碍**：TTS 朗读引擎、显示大小、文字大小、高对比度、触控目标、动画开关；
- **通知**：
  - 今日摘要开关与推送时间；
  - 实时活动（Android 16 锁屏阅读进度，目标默认 10 分钟，满后显示"已达成阅读目标🥳"；低于 Android 16 显示为灰色不可用）。

---

## 10. 常见问题

**Q：收不到更新？**
A：下拉刷新一次；检查网络；在订阅管理中确认该来源的同步频率；部分来源本身更新较慢。

**Q：添加了网址却提示找不到 RSS？**
A：按第 3 节方法确认 RSS 地址；或换用网页链接让 App 自动发现；公共 RSSHub 实例可能不稳定，可稍后重试。

**Q：文章图片不显示？**
A：检查是否开启了仅 Wi-Fi 加载图片；图片失效时会自动切换为无图卡片。

**Q：数据会上传吗？**
A：不会。订阅和文章保存在本机，金句离线生成。

**Q：换手机怎么办？**
A：先用"OPML 导出"备份订阅，在新手机安装 INTENT 后导入即可。

**Q：小组件颜色没变？**
A：在 App 内改色后会自动刷新；如启动器缓存，可删除小组件重新添加。

---

*祝你在 INTENT 里，读得专注。*
