package com.example.seedie.ui.screens.learning.listening

// Reviewed source quotations. Exact matching prevents stale annotations from highlighting unrelated text.
internal val listeningEvidenceQuotes: Map<String, List<String>> = mapOf(
    "original-g7-20260922-library-club-q1" to listOf("We meet in Room Twelve, not in the library, because the library is being cleaned this week."),
    "original-g7-20260922-library-club-q2" to listOf("Bring a pencil to write down new words."),
    "original-g7-20260922-library-club-q3" to listOf("The meeting ends at five, so you can take the school bus home."),
    "original-g7-20260922-lost-bag-q1" to listOf("We have found a blue schoolbag near the swimming pool."),
    "original-g7-20260922-lost-bag-q2" to listOf("If it is yours, please come to the front desk before six this evening."),
    "original-g7-20260922-lost-bag-q3" to listOf("You will need to tell him the name written inside the bag."),
    "original-g7-20260922-picnic-plan-q1" to listOf("Our picnic is on Sunday, not Saturday."),
    "original-g7-20260922-picnic-plan-q2" to listOf("Could you bring drinking water?"),
    "original-g7-20260922-picnic-plan-q3" to listOf("Our picnic is on Sunday, not Saturday.", "My brother has a football game on Saturday, and we want him to come too."),
    "original-g7-20260922-fruit-shop-q1" to listOf("Mum wanted four oranges, but there were only two left.", "I bought the two oranges for four yuan and a small bag of apples for five yuan."),
    "original-g7-20260922-fruit-shop-q2" to listOf("I bought the two oranges for four yuan and a small bag of apples for five yuan."),
    "original-g7-20260922-fruit-shop-q3" to listOf("I carried the fruit home in my schoolbag because I did not want another plastic bag."),
    "original-g8-20260922-museum-trip-q1" to listOf("The train service has been cancelled, so we will travel by coach instead."),
    "original-g8-20260922-museum-trip-q2" to listOf("Meet outside school at eight fifteen, fifteen minutes earlier than planned."),
    "original-g8-20260922-museum-trip-q3" to listOf("However, the cafe will be closed, so bring a packed lunch."),
    "original-g8-20260922-healthy-routine-q1" to listOf("Last month, I often felt sleepy in my first lesson."),
    "original-g8-20260922-healthy-routine-q2" to listOf("Now I leave my phone in the living room at nine and read for twenty minutes before bed."),
    "original-g8-20260922-healthy-routine-q3" to listOf("Now I leave my phone in the living room at nine and read for twenty minutes before bed.", "After two weeks, I can listen more carefully in class.", "Changing my bedtime has helped more than changing my food."),
    "original-g8-20260922-garden-project-q1" to listOf("She explained that the soil was already wet and that too much water could harm the plants."),
    "original-g8-20260922-garden-project-q2" to listOf("Now we check the soil before watering."),
    "original-g8-20260922-garden-project-q3" to listOf("We also keep a notebook of changes each week.", "In June, we picked our first tomatoes and used the notebook to explain what we had learned."),
    "original-g8-20260922-book-exchange-q1" to listOf("For each book you bring, you receive one ticket, which you can exchange for another book."),
    "original-g8-20260922-book-exchange-q2" to listOf("School workbooks are not accepted because many already have answers written in them."),
    "original-g8-20260922-book-exchange-q3" to listOf("The exchange opens at ten, but volunteers should arrive at nine thirty to arrange the tables."),
    "original-g9-20260922-repair-cafe-q1" to listOf("A volunteer found a loose wire and repaired it in ten minutes."),
    "original-g9-20260922-repair-cafe-q2" to listOf("There was no charge, although visitors could leave a donation."),
    "original-g9-20260922-repair-cafe-q3" to listOf("More importantly, she showed me how to check the lamp safely next time.", "I left with a working lamp and a useful skill.", "I now think twice before throwing things away."),
    "original-g9-20260922-travel-choice-q1" to listOf("The flight itself was only an hour, while the train journey took three hours."),
    "original-g9-20260922-travel-choice-q2" to listOf("The railway station was a short bus ride away."),
    "original-g9-20260922-travel-choice-q3" to listOf("However, the airport was far from our home, and we would need to arrive two hours before departure.", "The railway station was a short bus ride away.", "When we counted the whole journey, the train was more convenient.", "It also cost less.", "We chose the train, even though the flight looked faster at first."),
    "original-g9-20260922-online-survey-q1" to listOf("At first, we posted a survey only in the school reading club's online group."),
    "original-g9-20260922-online-survey-q2" to listOf("Our teacher pointed out that this group did not represent every student."),
    "original-g9-20260922-online-survey-q3" to listOf("We then asked students from each year group during lunch, including those who rarely used the internet.", "The results became more balanced.", "We learned that who answers a survey can be as important as the questions themselves."),
    "original-g9-20260922-food-waste-q1" to listOf("Our school tried to reduce food waste by putting up posters, but the amount of waste hardly changed."),
    "original-g9-20260922-food-waste-q2" to listOf("The kitchen began offering a smaller portion at a lower price, with extra food available if needed."),
    "original-g9-20260922-food-waste-q3" to listOf("Many said the standard portions were too large, especially on days when they were not very hungry.", "The kitchen began offering a smaller portion at a lower price, with extra food available if needed.", "After a month, waste had fallen.", "The council concluded that making a practical change was more effective than simply reminding people to waste less."),
    "l08-01-q1" to listOf("Amy: I signed up for the library helper group."),
    "l08-01-q2" to listOf("Amy: They meet on Saturday morning, right?", "Leo: Yes, from 9 to 11."),
    "l08-01-q3" to listOf("But I also want to improve my speaking skills.", "Amy: In that case, the museum guide team may help.", "They welcome visitors and answer simple questions."),
)

internal data class ListeningTranscriptSegment(val text: String, val questionIds: List<String>)

internal fun listeningTranscriptSegments(
    transcript: String,
    questionIds: List<String>,
    quotes: Map<String, List<String>> = listeningEvidenceQuotes
): List<ListeningTranscriptSegment> {
    data class Evidence(val start: Int, val end: Int, val questionId: String)
    val evidence = questionIds.flatMap { id ->
        quotes[id].orEmpty().mapNotNull { quote ->
            val start = transcript.indexOf(quote)
            if (quote.isBlank() || start < 0) null else Evidence(start, start + quote.length, id)
        }
    }
    val boundaries = (listOf(0, transcript.length) + evidence.flatMap { listOf(it.start, it.end) }).distinct().sorted()
    return boundaries.zipWithNext().map { (start, end) ->
        ListeningTranscriptSegment(transcript.substring(start, end),
            evidence.filter { it.start <= start && it.end >= end }.map { it.questionId }.distinct())
    }
}

internal fun listeningReviewPageCount(questionCount: Int, review: Boolean): Int =
    questionCount + if (review) 1 else 0
