package com.example;

import java.util.*;

public class PokerAI {
    private Random random;
    private int aggressionLevel; // 1-10, higher means more aggressive
    private static final double FOLD_THRESHOLD = 0.15; // Lower threshold to make AI fold less
    private static final double RAISE_THRESHOLD = 0.4; // Lower threshold to make AI raise more
    private int consecutiveFolds = 0; // Track consecutive folds to prevent too much folding

    public PokerAI() {
        this.random = new Random();
        this.aggressionLevel = 7 + random.nextInt(4); // More aggressive (7-10)
    }

    public enum Action {
        FOLD, CALL, RAISE
    }

    public Action decideAction(List<Card> hand, List<Card> communityCards, int currentBet, int potSize) {
        double handStrength = calculateHandStrength(hand, communityCards);
        double potOdds = (double) currentBet / (potSize + currentBet);

        // Prevent excessive folding
        if (consecutiveFolds >= 2) {
            consecutiveFolds = 0;
            return random.nextDouble() < 0.7 ? Action.CALL : Action.RAISE;
        }

        // More aggressive with any decent hand
        if (handStrength > RAISE_THRESHOLD) {
            if (random.nextDouble() < 0.8) {
                return Action.RAISE;
            }
            return Action.CALL;
        }

        // More likely to call with medium hands
        if (handStrength > FOLD_THRESHOLD) {
            if (random.nextDouble() < 0.4) {
                return Action.RAISE; // Occasional semi-bluff
            }
            return Action.CALL;
        }

        // Even with weak hands, sometimes call or bluff
        if (random.nextDouble() < 0.3 || potOdds < 0.2) {
            if (random.nextDouble() < 0.2) {
                return Action.RAISE; // Occasional pure bluff
            }
            return Action.CALL;
        }

        // Only fold if really bad hand and significant bet
        consecutiveFolds++;
        return Action.FOLD;
    }

    public int decideRaiseAmount(int currentBet, int potSize) {
        // More aggressive raise sizing
        int minRaise = currentBet * 2;
        int maxRaise = Math.min(currentBet * 5, potSize * 2); // Allow bigger raises

        // Occasionally make small raises to induce action
        if (random.nextDouble() < 0.2) {
            return minRaise;
        }

        // Sometimes make large raises
        if (random.nextDouble() < 0.3) {
            return maxRaise;
        }

        // Usually make medium-sized raises
        return minRaise + (maxRaise - minRaise) / 2;
    }

    private double calculateHandStrength(List<Card> hand, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>(hand);
        allCards.addAll(communityCards);

        Map<Integer, Integer> valueCount = new HashMap<>();
        Map<String, Integer> suitCount = new HashMap<>();

        int highestCard = 0;
        for (Card c : allCards) {
            valueCount.merge(c.getValue(), 1, Integer::sum);
            suitCount.merge(c.getSuit(), 1, Integer::sum);
            highestCard = Math.max(highestCard, c.getValue());
        }

        double strength = 0.0;

        // Check for various hand strengths
        boolean hasPair = valueCount.values().stream().anyMatch(v -> v >= 2);
        boolean hasTrips = valueCount.values().stream().anyMatch(v -> v >= 3);
        boolean hasFlushDraw = suitCount.values().stream().anyMatch(v -> v >= 4);
        boolean hasStraightDraw = hasStraightDraw(allCards);

        // Calculate base strength - higher values overall
        if (hasTrips) {
            strength = 0.9;
        } else if (hasPair) {
            int pairValue = valueCount.entrySet().stream()
                    .filter(e -> e.getValue() >= 2)
                    .mapToInt(Map.Entry::getKey)
                    .max()
                    .orElse(0);
            strength = 0.5 + (pairValue / 14.0) * 0.3;
        } else if (hasFlushDraw) {
            strength = 0.45;
        } else if (hasStraightDraw) {
            strength = 0.4;
        } else {
            // Higher base value for high cards
            strength = 0.2 + (highestCard / 14.0) * 0.3;
        }

        // Value high cards more
        if (highestCard >= 12) { // Queen or better
            strength += 0.1;
        }

        // Adjust strength based on stage
        if (communityCards.isEmpty()) {
            strength *= 1.3; // More optimistic pre-flop
        } else if (communityCards.size() <= 3) {
            strength *= 1.2; // More optimistic on flop
        }

        return Math.min(1.0, strength);
    }

    private boolean hasStraightDraw(List<Card> cards) {
        if (cards.size() < 4)
            return false;

        List<Integer> values = cards.stream()
                .map(Card::getValue)
                .sorted()
                .distinct()
                .toList();

        // Check for 4 consecutive cards or ace-low straight draw
        for (int i = 0; i < values.size() - 3; i++) {
            if (values.get(i + 3) - values.get(i) == 3) {
                return true;
            }
        }

        // Check for ace-low straight draw
        if (values.contains(14)) { // Ace
            List<Integer> withAceLow = new ArrayList<>(values);
            withAceLow.add(1); // Add ace as 1
            Collections.sort(withAceLow);
            for (int i = 0; i < withAceLow.size() - 3; i++) {
                if (withAceLow.get(i + 3) - withAceLow.get(i) == 3) {
                    return true;
                }
            }
        }

        return false;
    }
}