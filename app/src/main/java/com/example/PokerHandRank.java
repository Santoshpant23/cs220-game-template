package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PokerHandRank {
    public final String name;
    public final int value;

    public PokerHandRank(String n, int v) {
        name = n;
        value = v;
    }

    public static PokerHandRank evaluate(List<Card> cards) {
        Map<Integer, Integer> cnt = new HashMap<>();
        for (Card c : cards)
            cnt.merge(c.getValue(), 1, Integer::sum);
        boolean trips = cnt.values().stream().anyMatch(x -> x >= 3);
        long pairs = cnt.values().stream().filter(x -> x >= 2).count();
        if (trips)
            return new PokerHandRank("Three of a Kind", 4);
        if (pairs >= 2)
            return new PokerHandRank("Two Pair", 3);
        if (pairs == 1)
            return new PokerHandRank("Pair", 2);
        int high = cnt.keySet().stream().mapToInt(i -> i).max().orElse(0);
        return new PokerHandRank("High Card", 1);
    }
}