package com.example;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private List<Card> hand;
    private int chips;
    private String name;

    public Player(String name, int initialChips) {
        this.name = name;
        this.chips = initialChips;
        this.hand = new ArrayList<>();
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void clearHand() {
        hand.clear();
    }

    public List<Card> getHand() {
        return hand;
    }

    public int getChips() {
        return chips;
    }

    public void addChips(int amount) {
        chips += amount;
    }

    public void setChips(int amount) {
        this.chips = amount;
    }

    public boolean removeChips(int amount) {
        if (chips >= amount) {
            chips -= amount;
            return true;
        }
        return false;
    }

    public String getName() {
        return name;
    }
}