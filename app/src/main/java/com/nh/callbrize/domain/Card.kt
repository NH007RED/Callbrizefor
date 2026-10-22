package com.nh.callbrize.domain

import kotlin.random.Random

/** Suits are declared in an order that alternates colours, which makes a sorted hand easy to read. */
enum class Suit(val symbol: String, val isRed: Boolean) {
    SPADES("♠", false),
    HEARTS("♥", true),
    CLUBS("♣", false),
    DIAMONDS("♦", true)
}

enum class Rank(val label: String, val value: Int) {
    TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5), SIX("6", 6),
    SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9), TEN("10", 10),
    JACK("J", 11), QUEEN("Q", 12), KING("K", 13), ACE("A", 14)
}

data class Card(val suit: Suit, val rank: Rank) {
    val label: String get() = rank.label + suit.symbol
    val key: String get() = suit.name + "_" + rank.name
    val spoken: String get() = rank.label + " of " + suit.name.lowercase()
}

/** A card played by a seat into the current trick. */
data class Play(val seat: Int, val card: Card)

object Deck {
    fun standard(): List<Card> =
        Suit.values().flatMap { s -> Rank.values().map { r -> Card(s, r) } }

    fun shuffled(random: Random): List<Card> = standard().shuffled(random)
}
