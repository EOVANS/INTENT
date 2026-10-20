package com.intent.reader.quote

/**
 * Built-in lightweight lexicon for quote scoring (doc section 6).
 * Strong-opinion / emotion words only; no NLP models.
 */
object SentimentLexicon {

    val words: List<String> = listOf(
        // English
        "freedom", "truth", "change", "future", "destiny", "courage", "hope",
        "faith", "justice", "liberty", "power", "love", "hate", "fear", "dream",
        "wisdom", "knowledge", "beauty", "silence", "time", "memory", "history",
        "revolution", "war", "peace", "crisis", "collapse", "rebirth", "empire",
        "civilization", "humanity", "society", "culture", "art", "science",
        "genius", "madness", "glory", "shame", "pride", "greed", "envy",
        "loyalty", "betrayal", "sacrifice", "survival", "extinction", "climate",
        "planet", "universe", "infinity", "eternity", "fragile", "invisible",
        "extraordinary", "inevitable", "profound", "radical", "extreme",
        "impossible", "necessary", "dangerous", "brilliant", "terrible",
        "beautiful", "ugly", "secret", "mystery", "magic", "miracle",
        "collapse", "breakthrough", "disaster", "tragedy", "victory", "defeat",
        "legend", "icon", "vision", "mission", "believe", "believed",
        "never", "always", "forever", "everything", "nothing", "everyone",
        "alone", "together", "lost", "found", "born", "die", "died", "death",
        "life", "living", "soul", "heart", "mind", "spirit", "ghost",
        "shadow", "light", "darkness", "storm", "fire", "ice", "ocean",
        "mountain", "river", "stars", "moon", "sun",
        // Chinese
        "自由", "真相", "改变", "未来", "命运", "勇气", "希望", "信仰",
        "正义", "权力", "爱", "恨", "恐惧", "梦想", "智慧", "知识",
        "美", "沉默", "时间", "记忆", "历史", "革命", "战争", "和平",
        "危机", "崩溃", "重生", "帝国", "文明", "人类", "社会", "文化",
        "艺术", "科学", "天才", "疯狂", "荣耀", "耻辱", "骄傲", "贪婪",
        "嫉妒", "忠诚", "背叛", "牺牲", "生存", "毁灭", "气候", "星球",
        "宇宙", "永恒", "脆弱", "无形", "非凡", "必然", "深刻", "激进",
        "极端", "不可能", "必要", "危险", "辉煌", "可怕", "美丽", "丑陋",
        "秘密", "神秘", "魔法", "奇迹", "突破", "灾难", "悲剧", "胜利",
        "失败", "传说", "偶像", "愿景", "使命", "相信", "从不", "永远",
        "一切", "虚无", "所有人", "孤独", "失落", "重生", "死亡", "生命",
        "灵魂", "心灵", "精神", "幽灵", "阴影", "光明", "黑暗", "风暴",
        "火焰", "冰雪", "海洋", "山川", "河流", "星辰", "月亮", "太阳",
        "时代", "世界", "人生", "人心", "人性", "命运", "宿命", "选择",
        "告别", "重逢", "远方", "故乡", "少年", "青春", "衰老", "成长",
        // Japanese common
        "自由", "真実", "未来", "運命", "勇気", "希望", "平和", "革命",
        "時代", "世界", "人生", "心", "魂", "光", "闇", "夢",
        // Korean common
        "자유", "진실", "미래", "운명", "용기", "희망", "평화", "혁명",
        "시대", "세계", "인생", "영혼", "빛", "어둠", "꿈",
    ).distinct()

    /** Words/phrases that disqualify a sentence as quote material. */
    val adPatterns: List<String> = listOf(
        "subscribe", "click here", "sign up", "download app", "download the app",
        "newsletter", "sponsored", "advertis", "read more", "learn more",
        "广告", "点击这里", "点击查看", "订阅我们", "下载 app", "下载app",
        "注册会员", "扫码", "关注公众号", "阅读原文", "查看更多", "了解更多",
        "購読", "登録はこちら", "구독하기", "자세히 보기",
    )

    /** Sentence-leading photo caption markers. */
    val captionPatterns: List<String> = listOf(
        "photo:", "photo ", "picture:", "credit:", "getty", "reuters/",
        "afp", "ap photo", "image:", "图片来源", "图：", "图为", "图片来源：",
        "写真：", "사진:",
    )

    val quoteMarks = listOf("“", "”", "\"", "「", "」", "『", "』", "‘", "’")
}
