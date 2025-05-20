package com.RDragon.rd_translator

import android.content.Context

object IgnoreRepository {
    private const val PREFS = "IgnorePrefs"
    private const val KEY = "ignored"
    private val defaultWords = setOf(
        // Core A1 words
        "in", "you", "i", "the", "and", "to", "of", "a", "is", "that", "it", "he", "she", "we", "they",
        "at", "on", "for", "with", "as", "his", "her", "my", "your", "me", "not", "this", "but", "or", "from", "by",
        "have", "has", "had", "do", "did", "will", "can", "what", "when", "where", "why", "how",
        "if", "so", "there", "here", "out", "up", "down", "go", "come", "see", "say", "like", "good", "bad",
        "new", "old", "big", "small", "time", "day", "people", "one", "two", "three", "some", "all", "no", "yes",
        "more", "other", "them", "us", "our", "their", "make", "get", "know", "think", "take", "because", "very", "now",

        // Additional confirmed A1 words
        "about", "after", "afternoon", "again", "am", "an", "animal", "answer", "any", "apple", "april", "are", "arm", "ask", "august", "aunt", "autumn", "away",
        "baby", "back", "bag", "ball", "banana", "band", "baseball", "basket", "basketball", "bath", "bathroom", "be", "beach", "bean", "bear", "beautiful", "bed", "bedroom", "bee", "beef", "beer", "before", "begin", "behind", "belt", "best", "better", "bicycle", "bike", "bill", "bird", "birthday", "biscuit", "bit", "black", "blouse", "blue", "board", "boat", "body", "book", "boot", "born", "bottle", "bowl", "box", "boy", "bread", "breakfast", "brother", "brown", "bus", "butter", "buy", "bye",
        "cafe", "cake", "call", "camera", "camp", "candle", "cap", "car", "card", "careful", "carrot", "cat", "catch", "cd", "cent", "chair", "chips", "chocolate", "class", "classroom", "clean", "click", "clock", "close", "closed", "clothes", "cloud", "cloudy", "club", "coat", "coffee", "coin", "cold", "collect", "color", "comb", "computer", "cook", "cool", "copy", "correct", "count", "country", "cousin", "cow", "cream", "crocodile", "cup", "cupboard",
        "dad", "dance", "dancer", "dancing", "dark", "date", "daughter", "dear", "december", "dentist", "desk", "dinner", "dirty", "dish", "doctor", "dog", "dolphin", "door", "downstairs", "draw", "dress", "drink", "drive", "driver", "duck", "dvd",
        "ear", "early", "easy", "eat", "egg", "eight", "eighteen", "eighty", "elephant", "eleven", "email", "english", "eraser", "evening", "every", "everybody", "everyone", "everything", "everywhere", "exercise", "eye",
        "face", "family", "farm", "farmer", "fast", "fat", "father", "favorite", "february", "fifth", "fifteen", "fifty", "film", "find", "fine", "finger", "finish", "first", "fish", "fishing", "five", "flat", "floor", "flower", "fly", "fog", "foggy", "food", "foot", "football", "fork", "forty", "four", "fourteen", "fourth", "friday", "fridge", "friend", "friendly", "frog", "front", "fruit", "fun", "funny",
        "game", "garden", "gift", "girl", "give", "glad", "glass", "glasses", "goat", "gold", "golf", "grandfather", "grandmother", "grandparent", "grass", "gray", "green", "ground", "guitar", "gym",
        "hair", "half", "hand", "happy", "hard", "hat", "hello", "help", "hi", "hill", "hobby", "hockey", "holiday", "homework", "horse", "hospital", "hot", "hotel", "hour", "house", "hundred", "hungry", "husband",
        "ice", "ice cream", "ill", "internet", "into",
        "jacket", "jam", "january", "jeans", "job", "juice", "july", "jump", "jumper", "june",
        "key", "keyboard", "kick", "kid", "kilo", "kilometer", "kind", "kitchen", "kite", "knock",
        "lake", "lamp", "laptop", "large", "last", "late", "later", "learn", "leave", "left", "leg", "lemon", "lemonade", "lesson", "letter", "life", "light", "lion", "list", "listen", "little", "live", "living room", "long", "look", "lost", "lot", "love", "lovely", "lunch",
        "map", "march", "market", "maths", "may", "maybe", "meal", "meat", "meet", "menu", "meter", "midnight", "milk", "mirror", "mix", "monday", "money", "monkey", "month", "moon", "morning", "most", "mother", "motorbike", "mountain", "mouse", "mouth", "move", "mr", "mrs", "ms", "mum", "museum", "music",
        "name", "near", "neck", "need", "never", "night", "nine", "nineteen", "ninety", "no one", "nobody", "nothing", "nowhere", "noon", "nose", "notebook", "november", "number", "nurse",
        "o'clock", "october", "often", "oh", "ok", "once", "onion", "online", "open", "orange",
        "page", "pair", "painting", "paper", "parent", "park", "party", "pasta", "path", "pen", "pencil", "penny", "pepper", "pet", "phone", "photo", "photograph", "piano", "picture", "pig", "pilot", "pine", "pink", "place", "plane", "plant", "plate", "play", "please", "pm", "pocket", "policeman", "police", "poor", "pop", "postcard", "poster", "potato", "pound", "present", "printer", "puzzle", "pyjamas",
        "question", "quick", "quickly", "quiet",
        "rabbit", "radio", "rain", "read", "reader", "reading", "ready", "record", "red", "refrigerator", "repeat", "restaurant", "rice", "ride", "right", "river", "road", "robot", "room", "rubber", "rugby", "run", "runner", "running",
        "sad", "sail", "sailing", "salad", "salt", "same", "sandwich", "saturday", "sausage", "scarf", "school", "scissors", "screen", "sea", "seat", "second", "send", "september", "seven", "seventeen", "seventy", "shark", "sheep", "sheet", "shelf", "shirt", "shoe", "shop", "shopping", "short", "shorts", "shout", "show", "shower", "sick", "silver", "sing", "singer", "singing", "sink", "sister", "sit", "six", "sixteen", "sixty", "size", "skate", "skateboard", "ski", "skiing", "skirt", "sky", "sleep", "slice", "slow", "slowly", "smart", "smell", "smoke", "smoking", "snack", "snake", "snow", "soap", "soccer", "sock", "sofa", "song", "sorry", "soup", "speak", "spell", "spend", "spoon", "sport", "spring", "stairs", "stamp", "stand", "star", "station", "stomach", "stop", "storm", "story", "street", "student", "studies", "study", "sugar", "suitcase", "summer", "sun", "sunday", "sunny", "supermarket", "supper", "surf", "surname", "sweater", "sweet", "swim", "swimming", "switch",
        "table", "tablet", "tall", "taxi", "tea", "teach", "teacher", "telephone", "television", "tell", "ten", "tennis", "tent", "text", "thanks", "then", "thirsty", "thirteen", "thirty", "thousand", "throw", "thursday", "ticket", "tidy", "tiger", "tired", "toilet", "tomato", "tomorrow", "tonight", "too", "tooth", "toothbrush", "touch", "towel", "town", "toy", "train", "trainer", "tree", "trousers", "t-shirt", "tuesday", "turn", "tv", "twelve", "twenty", "type",
        "umbrella", "uncle", "under", "understand", "upstairs", "use",
        "vegetable", "video", "visit", "visitor",
        "wait", "waiter", "wake", "walk", "wall", "wallet", "want", "warm", "wash", "watch", "water", "wear", "weather", "wednesday", "week", "weekend", "welcome", "well", "white", "who", "why", "wife", "win", "winner", "wind", "window", "windy", "winter", "woman", "word", "work", "wow", "write", "wrong",
        "yellow", "yesterday", "young",
        "zebra", "zero", "zoo"
    )

    fun initDefaults(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY)) {
            prefs.edit().putStringSet(KEY, defaultWords).apply()
        }
    }

    fun getIgnored(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, defaultWords) ?: defaultWords

    fun addIgnored(ctx: Context, word: String) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = getIgnored(ctx).toMutableSet().apply { add(word.lowercase()) }
        prefs.edit().putStringSet(KEY, set).apply()
    }
}