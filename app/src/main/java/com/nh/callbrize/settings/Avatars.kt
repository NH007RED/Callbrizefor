package com.nh.callbrize.settings

/** Built-in avatars (emoji, so no image assets or licensing are needed). */
object Avatars {
    val emojis = listOf("🦁", "🐯", "🦅", "🐼", "🦊", "🐺", "👑", "🎩")
    fun emoji(index: Int): String = emojis[((index % emojis.size) + emojis.size) % emojis.size]
}
