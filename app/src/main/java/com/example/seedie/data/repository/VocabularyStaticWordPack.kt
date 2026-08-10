package com.example.seedie.data.repository

import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.WordBookEntity

internal data class StaticWordEntry(
    val wordId: String,
    val english: String,
    val phonetic: String,
    val partOfSpeech: String,
    val translation: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val difficultyLevel: String,
    val rewardToken: Int,
    val estimatedDurationSec: Int
)

internal object VocabularyStaticWordPack {
    const val DEFAULT_BOOK_ID = "seedie-default-book"

    private const val DEFAULT_BOOK_TITLE = "Seedie 默认词书"
    private const val DEFAULT_BOOK_DESCRIPTION = "内置基础词汇包，供本地练习与首启初始化使用"
    private const val DEFAULT_LANGUAGE = "en-US"
    private const val DEFAULT_DIFFICULTY = "mixed"
    private const val DEFAULT_VERSION = 1

    private val entries: List<StaticWordEntry> = listOf(
        StaticWordEntry("w1", "apple", "/ˈae.pəl/", "n.", "苹果", "An apple a day keeps the doctor away.", "一天一苹果，医生远离我。", "easy", 3, 8),
        StaticWordEntry("w2", "bridge", "/brɪdʒ/", "n.", "桥", "We walked across the bridge together.", "我们一起走过了那座桥。", "easy", 3, 8),
        StaticWordEntry("w3", "careful", "/ˈkeə.fəl/", "adj.", "小心的", "Please be careful with the glass bottle.", "拿玻璃瓶时请小心。", "easy", 3, 8),
        StaticWordEntry("w4", "garden", "/ˈɡɑː.dən/", "n.", "花园", "The children water flowers in the garden.", "孩子们在花园里给花浇水。", "easy", 3, 8),
        StaticWordEntry("w5", "smile", "/smaɪl/", "v.", "微笑", "She smiles when she sees her friends.", "她看到朋友时会微笑。", "easy", 3, 8),
        StaticWordEntry("w6", "forest", "/ˈfɒr.ɪst/", "n.", "森林", "A gentle wind moves through the forest.", "一阵微风穿过森林。", "easy", 3, 8),
        StaticWordEntry("w7", "bright", "/braɪt/", "adj.", "明亮的", "The classroom is bright and clean.", "教室既明亮又干净。", "easy", 3, 8),
        StaticWordEntry("w8", "listen", "/ˈlɪs.ən/", "v.", "听", "Please listen to the teacher carefully.", "请认真听老师讲。", "easy", 3, 8),
        StaticWordEntry("w9", "discover", "/dɪˈskʌv.ər/", "v.", "发现", "The children discover a tiny seed in the soil.", "孩子们在土里发现了一粒小小的种子。", "medium", 4, 10),
        StaticWordEntry("w10", "protect", "/prəˈtekt/", "v.", "保护", "Trees protect the soil from strong wind.", "树木保护土壤免受强风侵袭。", "medium", 4, 10),
        StaticWordEntry("w11", "ancient", "/ˈeɪn.ʃənt/", "adj.", "古老的", "We visited an ancient town in summer.", "夏天我们参观了一座古老的小镇。", "medium", 4, 10),
        StaticWordEntry("w12", "journey", "/ˈdʒɜː.ni/", "n.", "旅程", "Reading can begin a wonderful journey.", "阅读能开启一段美妙的旅程。", "medium", 4, 10),
        StaticWordEntry("w13", "whisper", "/ˈwɪs.pər/", "v.", "低语", "The leaves whisper in the night wind.", "叶子在夜风中低语。", "medium", 4, 10),
        StaticWordEntry("w14", "patient", "/ˈpeɪ.ʃənt/", "adj.", "耐心的", "A patient learner makes steady progress.", "有耐心的学习者会稳步进步。", "medium", 4, 10),
        StaticWordEntry("w15", "collect", "/kəˈlekt/", "v.", "收集", "We collect leaves for the science project.", "我们为科学课作业收集树叶。", "medium", 4, 10),
        StaticWordEntry("w16", "sudden", "/ˈsʌd.ən/", "adj.", "突然的", "A sudden rain cooled the playground.", "一场突然的雨让操场凉了下来。", "medium", 4, 10),
        StaticWordEntry("w17", "measure", "/ˈmeʒ.ər/", "v.", "测量", "Use the ruler to measure the desk.", "用尺子测量这张书桌。", "hard", 5, 12),
        StaticWordEntry("w18", "curious", "/ˈkjʊə.ri.əs/", "adj.", "好奇的", "The curious cat looked at the new toy.", "那只好奇的猫盯着新玩具看。", "hard", 5, 12),
        StaticWordEntry("w19", "improve", "/ɪmˈpruːv/", "v.", "提升", "Practice every day to improve your English.", "每天练习来提升你的英语。", "hard", 5, 12),
        StaticWordEntry("w20", "balance", "/ˈbæl.əns/", "n.", "平衡", "Riding a bike helps children build balance.", "骑自行车能帮孩子建立平衡感。", "hard", 5, 12),
        StaticWordEntry("w21", "gentle", "/ˈdʒen.təl/", "adj.", "温和的", "The rabbit has a gentle face.", "这只兔子有一张温和的脸。", "hard", 5, 12),
        StaticWordEntry("w22", "observe", "/əbˈzɜːv/", "v.", "观察", "Observe how the plant grows each day.", "观察这株植物每天是怎么长的。", "hard", 5, 12),
        StaticWordEntry("w23", "resource", "/rɪˈzɔːs/", "n.", "资源", "Books are an important learning resource.", "书籍是重要的学习资源。", "hard", 5, 12),
        StaticWordEntry("w24", "create", "/kriˈeɪt/", "v.", "创造", "Children create stories with colorful cards.", "孩子们用彩色卡片创造故事。", "hard", 5, 12)
    )

    fun defaultBookEntity(): WordBookEntity {
        return WordBookEntity(
            bookId = DEFAULT_BOOK_ID,
            title = DEFAULT_BOOK_TITLE,
            description = DEFAULT_BOOK_DESCRIPTION,
            language = DEFAULT_LANGUAGE,
            difficulty = DEFAULT_DIFFICULTY,
            version = DEFAULT_VERSION,
            sourceType = "bundled",
            downloadStatus = "downloaded",
            isActive = true,
            wordCount = entries.size,
            updatedAt = 1L
        )
    }

    fun defaultWordEntities(): List<VocabularyWordEntity> {
        return entries.mapIndexed { index, entry ->
            VocabularyWordEntity(
                wordId = entry.wordId,
                bookId = DEFAULT_BOOK_ID,
                english = entry.english,
                phonetic = entry.phonetic,
                partOfSpeech = entry.partOfSpeech,
                translation = entry.translation,
                exampleSentence = entry.exampleSentence,
                exampleTranslation = entry.exampleTranslation,
                difficultyLevel = entry.difficultyLevel,
                rewardToken = entry.rewardToken,
                estimatedDurationSec = entry.estimatedDurationSec,
                sortOrder = index,
                sensesJson = com.example.seedie.domain.model.WordSenseFormat.encodeSensesJson(
                    listOf(
                        com.example.seedie.domain.model.WordSense(
                            partOfSpeech = entry.partOfSpeech,
                            translation = entry.translation
                        )
                    )
                )
            )
        }
    }
}
