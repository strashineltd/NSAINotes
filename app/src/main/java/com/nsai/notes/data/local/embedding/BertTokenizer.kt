package com.nsai.notes.data.local.embedding

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer

/**
 * BERT WordPiece tokenizer compatible with all-MiniLM-L6-v2 (bert-base-uncased vocab).
 *
 * Requires a vocab.txt file in assets/embeddings/ (one token per line, line number = token ID).
 * The standard bert-base-uncased vocab has 30522 tokens.
 */
class BertTokenizer(private val context: Context) {

    private var vocab: Map<String, Int> = emptyMap()
    private var isLoaded = false

    companion object {
        const val MAX_LENGTH = 128
        const val CLS_ID = 101
        const val SEP_ID = 102
        const val UNK_ID = 100
        const val PAD_ID = 0
        private const val VOCAB_PATH = "embeddings/vocab.txt"
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        vocab = loadVocab()
        isLoaded = true
    }

    val initialized: Boolean get() = isLoaded

    /**
     * Tokenize text and return token IDs as a padded/truncated LongArray of [MAX_LENGTH].
     */
    suspend fun tokenize(text: String): LongArray = withContext(Dispatchers.IO) {
        if (!isLoaded) throw IllegalStateException("BertTokenizer not initialized")
        val result = LongArray(MAX_LENGTH) { PAD_ID.toLong() }

        val tokens = tokenizeToIds(text)
        result[0] = CLS_ID.toLong()
        for (i in tokens.indices) {
            if (i + 1 >= MAX_LENGTH) break
            result[i + 1] = tokens[i].toLong()
        }
        val sepPos = (tokens.size + 1).coerceAtMost(MAX_LENGTH - 1)
        result[sepPos] = SEP_ID.toLong()
        result
    }

    private fun tokenizeToIds(text: String): List<Int> {
        val normalized = normalizeText(text)
        val basicTokens = splitToBasicTokens(normalized)
        return basicTokens.flatMap { wordpieceTokenize(it) }
    }

    private fun normalizeText(text: String): String {
        var result = Normalizer.normalize(text, Normalizer.Form.NFKC)
        result = result.lowercase()
        // Normalize whitespace
        result = result.replace(Regex("\\s+"), " ")
        // Separate CJK characters
        result = separateCjk(result)
        // Separate punctuation
        result = separatePunctuation(result)
        return result.trim()
    }

    /**
     * Insert spaces around each CJK character so they become individual tokens.
     */
    private fun separateCjk(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (isCjk(ch)) {
                sb.append(' ').append(ch).append(' ')
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun isCjk(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x4E00..0x9FFF) ||  // CJK Unified Ideographs
               (code in 0x3400..0x4DBF) ||  // CJK Unified Ideographs Extension A
               (code in 0x20000..0x2A6DF) || // CJK Unified Ideographs Extension B
               (code in 0xF900..0xFAFF) ||  // CJK Compatibility Ideographs
               (code in 0x3040..0x309F) ||  // Hiragana
               (code in 0x30A0..0x30FF) ||  // Katakana
               (code in 0xAC00..0xD7AF)     // Hangul
    }

    /**
     * Insert spaces around punctuation characters.
     */
    private fun separatePunctuation(text: String): String {
        return text.replace(Regex("([\\p{Punct}&&[^_]])"), " $1 ")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Split normalized text into basic tokens by whitespace.
     */
    private fun splitToBasicTokens(text: String): List<String> {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    /**
     * WordPiece tokenization: longest-match-first greedy algorithm.
     * Unknown characters become [UNK].
     */
    private fun wordpieceTokenize(token: String): List<Int> {
        if (token.isEmpty()) return emptyList()
        val ids = mutableListOf<Int>()

        var remaining = token
        while (remaining.isNotEmpty()) {
            val id = findLongestMatch(remaining, ids.isEmpty())
            if (id != null) {
                ids.add(id)
                remaining = remaining.substring(remaining.length - getTokenLength(id, remaining, ids.size == 1))
            } else {
                // No match found → use [UNK] for the whole token
                ids.add(UNK_ID)
                break
            }
        }
        return ids
    }

    /**
     * Find the longest matching token ID for the given text.
     * First token in a word doesn't use ## prefix; subsequent tokens do.
     */
    private fun findLongestMatch(text: String, isFirst: Boolean): Int? {
        val effectiveLength = text.length.coerceAtMost(50) // upper bound for subword length
        for (end in effectiveLength downTo 1) {
            val candidate = if (isFirst) {
                text.substring(0, end)
            } else {
                "##${text.substring(0, end)}"
            }
            vocab[candidate]?.let { return it }
        }
        return null
    }

    /**
     * Get the original token text length that corresponds to a token ID.
     * Removes ## prefix for subword tokens.
     */
    private fun getTokenLength(id: Int, text: String, isFirst: Boolean): Int {
        // Find the token string from vocab
        val tokenStr = vocab.entries.firstOrNull { it.value == id }?.key ?: return 1
        val actual = if (isFirst || !tokenStr.startsWith("##")) {
            tokenStr.length
        } else {
            tokenStr.length - 2 // remove ##
        }
        return actual.coerceAtMost(text.length)
    }

    private fun loadVocab(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        context.assets.open(VOCAB_PATH).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                var id = 0
                var line = reader.readLine()
                while (line != null) {
                    val token = line.trim()
                    if (token.isNotEmpty()) {
                        map[token] = id
                    }
                    id++
                    line = reader.readLine()
                }
            }
        }
        return map
    }
}
