package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {
    private List<Card> cards;
    private static final String[] SUITS = { "Hearts", "Diamonds", "Clubs", "Spades" };
    private static final String[] RANKS = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A" };
    private Random random;

    public Deck() {
        cards = new ArrayList<>();
        random = new Random();
        initializeDeck();
    }

    private void initializeDeck() {
        cards.clear(); // Clear existing cards before initializing
        for (String suit : SUITS) {
            for (int i = 0; i < RANKS.length; i++) {
                cards.add(new Card(suit, RANKS[i], i + 2));
            }
        }
    }

    public void shuffle() {
        // Triple shuffle for better randomization
        for (int i = 0; i < 3; i++) {
            Collections.shuffle(cards, random);
        }
        // Cut the deck at a random point
        int cutPoint = random.nextInt(cards.size());
        List<Card> temp = new ArrayList<>(cards.subList(cutPoint, cards.size()));
        temp.addAll(cards.subList(0, cutPoint));
        cards = temp;
    }

    public Card drawCard() {
        if (cards.isEmpty()) {
            // If deck is empty, reinitialize and shuffle
            initializeDeck();
            shuffle();
        }
        return cards.remove(0);
    }

    public int remainingCards() {
        return cards.size();
    }
}