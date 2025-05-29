package com.example;

import java.util.*;

public class PokerModel {
    public enum StageName {
        PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN
    }

    private Deck deck;
    private Player player, dealer;
    private PokerAI ai;
    private List<Card> communityList;
    private int currentBet, pot, callCount;
    private boolean isPlayerTurn, isPlayerDealer;
    private StageName stage;
    private String statusMessage;

    public PokerModel() {
        deck = new Deck();
        player = new Player("You", 1000);
        dealer = new Player("Dealer", 1000);
        ai = new PokerAI();
        communityList = new ArrayList<>();
        resetGame();
    }

    public void resetGame() {
        player.setChips(1000);
        dealer.setChips(1000);
        player.clearHand();
        dealer.clearHand();
        communityList.clear();
        isPlayerDealer = true;
        isPlayerTurn = true;
        stage = StageName.PRE_FLOP;
        pot = callCount = 0;
        statusMessage = "Welcome!";
    }

    public void startHand() {
        deck.shuffle();
        communityList.clear();
        player.clearHand();
        dealer.clearHand();
        isPlayerDealer = !isPlayerDealer;
        stage = StageName.PRE_FLOP;
        pot = callCount = 0;
        for (int i = 0; i < 3; i++) {
            player.addCard(deck.drawCard());
            dealer.addCard(deck.drawCard());
        }
        for (int i = 0; i < 5; i++) {
            Card c = deck.drawCard();
            c.setFaceUp(false);
            communityList.add(c);
        }
        currentBet = 10;
        if (isPlayerDealer) {
            player.removeChips(currentBet);
            dealer.removeChips(currentBet * 2);
            isPlayerTurn = true;
        } else {
            dealer.removeChips(currentBet);
            player.removeChips(currentBet * 2);
            isPlayerTurn = false;
        }
        pot = currentBet * 3;
        statusMessage = (isPlayerDealer ? "Dealer" : "You") + " posted blinds. Pre-flop.";
    }

    public void playerFold() {
        if (!isPlayerTurn || stage == StageName.SHOWDOWN)
            return;
        dealer.addChips(pot);
        statusMessage = "You folded. Dealer wins.";
        finishShowdown();
    }

    public void playerCall() {
        if (!isPlayerTurn || stage == StageName.SHOWDOWN)
            return;
        if (!player.removeChips(currentBet)) {
            statusMessage = "Not enough chips to call.";
            return;
        }
        pot += currentBet;
        callCount++;
        statusMessage = "You called " + currentBet;
        if (callCount >= 2)
            nextStage();
        else {
            isPlayerTurn = false;
        }
    }

    public void playerRaise(int amt) {
        if (!isPlayerTurn || stage == StageName.SHOWDOWN)
            return;
        if (amt <= currentBet) {
            statusMessage = "Raise must exceed " + currentBet;
            return;
        }
        if (!player.removeChips(amt)) {
            statusMessage = "Not enough chips to raise.";
            return;
        }
        currentBet = amt;
        pot += amt;
        callCount = 1;
        statusMessage = "You raised to " + amt;
        isPlayerTurn = false;
    }

    public void aiTurn() {
        if (isPlayerTurn || stage == StageName.SHOWDOWN)
            return;
        PokerAI.Action act = ai.decideAction(dealer.getHand(), visibleCommunity(), currentBet, pot);
        switch (act) {
            case FOLD:
                player.addChips(pot);
                statusMessage = "Dealer folded. You win!";
                finishShowdown();
                break;
            case CALL:
                dealer.removeChips(currentBet);
                pot += currentBet;
                callCount++;
                statusMessage = "Dealer called.";
                if (callCount >= 2)
                    nextStage();
                else {
                    isPlayerTurn = true;
                }
                break;
            case RAISE:
                int r = ai.decideRaiseAmount(currentBet, pot, dealer.getChips());
                dealer.removeChips(r);
                pot += r;
                currentBet = r;
                callCount = 1;
                statusMessage = "Dealer raised to " + r;
                isPlayerTurn = true;
                break;
        }
    }

    public void nextStage() {
        switch (stage) {
            case PRE_FLOP:
                for (int i = 0; i < 3; i++)
                    communityList.get(i).setFaceUp(true);
                stage = StageName.FLOP;
                break;
            case FLOP:
                communityList.get(3).setFaceUp(true);
                stage = StageName.TURN;
                break;
            case TURN:
                communityList.get(4).setFaceUp(true);
                stage = StageName.SHOWDOWN;
                finishShowdown();
                return;
            default:
                return;
        }
        callCount = 0;
        currentBet = 0;
        isPlayerTurn = !isPlayerDealer;
        statusMessage = stage.name() + " - " + (isPlayerTurn ? "Your turn" : "Dealer's turn");
    }

    public void finishShowdown() {
        dealer.getHand().forEach(c -> c.setFaceUp(true));
        communityList.forEach(c -> c.setFaceUp(true));
        evaluateWinner();
        stage = StageName.SHOWDOWN;
    }

    public void evaluateWinner() {
        List<Card> pFull = new ArrayList<>(player.getHand());
        pFull.addAll(communityList);
        List<Card> dFull = new ArrayList<>(dealer.getHand());
        dFull.addAll(communityList);
        PokerHandRank hp = PokerHandRank.evaluate(pFull);
        PokerHandRank hd = PokerHandRank.evaluate(dFull);
        if (hp.value > hd.value) {
            player.addChips(pot);
            statusMessage = "You win with " + hp.name + "!";
        } else if (hd.value > hp.value) {
            dealer.addChips(pot);
            statusMessage = "Dealer wins with " + hd.name + "!";
        } else {
            statusMessage = "Tie: " + hp.name + "!";
        }
        pot = 0;
    }

    public List<Card> visibleCommunity() {
        List<Card> v = new ArrayList<>();
        for (Card c : communityList)
            if (c.isFaceUp())
                v.add(c);
        return v;
    }

    // Getters for UI
    public Player getPlayer() {
        return player;
    }

    public Player getDealer() {
        return dealer;
    }

    public List<Card> getCommunityList() {
        return communityList;
    }

    public int getPot() {
        return pot;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public boolean isPlayerTurn() {
        return isPlayerTurn;
    }

    public boolean isPlayerDealer() {
        return isPlayerDealer;
    }

    public StageName getStage() {
        return stage;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}