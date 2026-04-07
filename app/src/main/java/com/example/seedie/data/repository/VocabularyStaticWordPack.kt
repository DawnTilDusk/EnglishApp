package com.example.seedie.data.repository

internal data class StaticWordEntry(
    val wordId: String,
    val english: String,
    val phonetic: String,
    val partOfSpeech: String,
    val translation: String,
    val exampleSentence: String,
    val difficultyLevel: String,
    val rewardToken: Int,
    val estimatedDurationSec: Int
)

internal object VocabularyStaticWordPack {
    val entries: List<StaticWordEntry> = listOf(
        StaticWordEntry("w1", "apple", "/ˈae.pəl/", "n.", "苹果", "An apple a day keeps the doctor away.", "easy", 3, 8),
        StaticWordEntry("w2", "bridge", "/brɪdʒ/", "n.", "桥", "We walked across the bridge together.", "easy", 3, 8),
        StaticWordEntry("w3", "careful", "/ˈkeə.fəl/", "adj.", "小心的", "Please be careful with the glass bottle.", "easy", 3, 8),
        StaticWordEntry("w4", "garden", "/ˈɡɑː.dən/", "n.", "花园", "The children water flowers in the garden.", "easy", 3, 8),
        StaticWordEntry("w5", "smile", "/smaɪl/", "v.", "微笑", "She smiles when she sees her friends.", "easy", 3, 8),
        StaticWordEntry("w6", "forest", "/ˈfɒr.ɪst/", "n.", "森林", "A gentle wind moves through the forest.", "easy", 3, 8),
        StaticWordEntry("w7", "bright", "/braɪt/", "adj.", "明亮的", "The classroom is bright and clean.", "easy", 3, 8),
        StaticWordEntry("w8", "listen", "/ˈlɪs.ən/", "v.", "听", "Please listen to the teacher carefully.", "easy", 3, 8),
        StaticWordEntry("w9", "discover", "/dɪˈskʌv.ər/", "v.", "发现", "The children discover a tiny seed in the soil.", "medium", 4, 10),
        StaticWordEntry("w10", "protect", "/prəˈtekt/", "v.", "保护", "Trees protect the soil from strong wind.", "medium", 4, 10),
        StaticWordEntry("w11", "ancient", "/ˈeɪn.ʃənt/", "adj.", "古老的", "We visited an ancient town in summer.", "medium", 4, 10),
        StaticWordEntry("w12", "journey", "/ˈdʒɜː.ni/", "n.", "旅程", "Reading can begin a wonderful journey.", "medium", 4, 10),
        StaticWordEntry("w13", "whisper", "/ˈwɪs.pər/", "v.", "低语", "The leaves whisper in the night wind.", "medium", 4, 10),
        StaticWordEntry("w14", "patient", "/ˈpeɪ.ʃənt/", "adj.", "耐心的", "A patient learner makes steady progress.", "medium", 4, 10),
        StaticWordEntry("w15", "collect", "/kəˈlekt/", "v.", "收集", "We collect leaves for the science project.", "medium", 4, 10),
        StaticWordEntry("w16", "sudden", "/ˈsʌd.ən/", "adj.", "突然的", "A sudden rain cooled the playground.", "medium", 4, 10),
        StaticWordEntry("w17", "measure", "/ˈmeʒ.ər/", "v.", "测量", "Use the ruler to measure the desk.", "hard", 5, 12),
        StaticWordEntry("w18", "curious", "/ˈkjʊə.ri.əs/", "adj.", "好奇的", "The curious cat looked at the new toy.", "hard", 5, 12),
        StaticWordEntry("w19", "improve", "/ɪmˈpruːv/", "v.", "提升", "Practice every day to improve your English.", "hard", 5, 12),
        StaticWordEntry("w20", "balance", "/ˈbæl.əns/", "n.", "平衡", "Riding a bike helps children build balance.", "hard", 5, 12),
        StaticWordEntry("w21", "gentle", "/ˈdʒen.təl/", "adj.", "温和的", "The rabbit has a gentle face.", "hard", 5, 12),
        StaticWordEntry("w22", "observe", "/əbˈzɜːv/", "v.", "观察", "Observe how the plant grows each day.", "hard", 5, 12),
        StaticWordEntry("w23", "resource", "/rɪˈzɔːs/", "n.", "资源", "Books are an important learning resource.", "hard", 5, 12),
        StaticWordEntry("w24", "create", "/kriˈeɪt/", "v.", "创造", "Children create stories with colorful cards.", "hard", 5, 12)
    )
}
