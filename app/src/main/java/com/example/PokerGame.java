package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.stage.Modality;

import java.util.*;

public class PokerGame extends Application {
    private Deck deck;
    private Player player, dealer;
    private PokerAI ai;

    private HBox playerCards, dealerCards, communityCards;
    private Label playerChips, dealerChips, potLabel, gameStatus;
    private Button dealButton, foldButton, callButton, raiseButton, exitGameButton;
    private Slider betSlider;

    private int currentBet = 0;
    private int pot = 0;
    private boolean isPlayerTurn;
    private boolean isPlayerDealer;

    private enum StageName {
        PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN
    }

    private StageName stage = StageName.PRE_FLOP;

    private List<Card> communityList = new ArrayList<>();
    private int callCount = 0;

    private StackPane root;
    private Scene scene;
    private VBox homeScreen, rulesScreen, tutorialScreen, gameScreenContainer;
    private BorderPane gameScreen;
    private Font pokerFont;

    @Override
    public void start(Stage primaryStage) {
        loadFont();
        root = new StackPane();
        scene = new Scene(root, 1000, 800);

        setupHomeScreen();
        setupRulesScreen();
        setupTutorialScreen();
        setupGameScreen();

        showHome();

        primaryStage.setTitle("Poker Game");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/KC.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadFont() {
        pokerFont = Font.font("Serif", FontWeight.BOLD, 24);
        try {
            Font f = Font.loadFont(getClass().getResourceAsStream("/assets/Play-Regular.ttf"), 24);
            if (f != null)
                pokerFont = f;
        } catch (Exception ignored) {
        }
    }

    private void setupHomeScreen() {
        homeScreen = new VBox(20);
        homeScreen.setAlignment(Pos.CENTER);
        homeScreen.setStyle("-fx-background-color: #2B4C1E;");
        homeScreen.setPadding(new Insets(60));

        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/assets/KC.png")));
        logo.setFitWidth(120);
        logo.setFitHeight(160);

        Label title = new Label("POKER GAME");
        title.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, 48));
        title.setTextFill(Color.web("#FFD700"));
        title.setStyle("-fx-effect: dropshadow(gaussian, black, 4, 0.5, 2, 2);");

        Button start = styledButton("Start Game", "#388E3C", e -> showGame());
        Button rules = styledButton("Rules", "#1976D2", e -> showRules());
        Button tut = styledButton("Tutorial", "#FFA000", e -> showTutorial());
        Button exit = styledButton("Exit", "#E53935", e -> Platform.exit());

        homeScreen.getChildren().setAll(logo, title, start, rules, tut, exit);
    }

    private void setupRulesScreen() {
        rulesScreen = new VBox(20);
        rulesScreen.setAlignment(Pos.CENTER);
        rulesScreen.setStyle("-fx-background-color: #2B4C1E;");
        rulesScreen.setPadding(new Insets(60));

        Label h = new Label("Poker Rules");
        h.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, 40));
        h.setTextFill(Color.web("#FFD700"));
        h.setStyle("-fx-effect: dropshadow(gaussian, black, 4, 0.5, 2, 2);");

        Label text = new Label(
                "1. Each player is dealt 3 cards.\n" +
                        "2. Community cards revealed in stages: Flop(3), Turn(1), River(1).\n" +
                        "3. Betting each round: Fold, Call, or Raise.\n" +
                        "4. Best 3-card hand wins at showdown.\n" +
                        "5. High card if no pairs/trips.\n" +
                        "6. Dealer’s cards hidden until showdown.\n" +
                        "7. Enjoy responsibly!");
        text.setFont(Font.font(pokerFont.getFamily(), FontWeight.NORMAL, 22));
        text.setTextFill(Color.WHITE);
        text.setWrapText(true);
        text.setAlignment(Pos.CENTER);
        text.setMaxWidth(700);

        Button back = styledButton("Back", "#f44336", e -> showHome());
        rulesScreen.getChildren().setAll(h, text, back);
    }

    private void setupTutorialScreen() {
        tutorialScreen = new VBox(20);
        tutorialScreen.setAlignment(Pos.CENTER_LEFT);
        tutorialScreen.setStyle("-fx-background-color: #2B4C1E;");
        tutorialScreen.setPadding(new Insets(40));

        Label h = new Label("Game Tutorial");
        h.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, 40));
        h.setTextFill(Color.web("#FFD700"));

        // Build up a String with \u2022 (bullet) and only straight ASCII characters
        // everywhere
        String tutorialText = "\u2022 Deal:   Starts a new hand - shuffles deck, posts blinds, deals 3 cards each.\n" +
                "\u2022 Fold:   Give up the round; opponent wins the pot.\n" +
                "\u2022 Call:   Match the current bet to stay in.\n" +
                "\u2022 Raise:  Increase the bet above the current amount.\n" +
                "\u2022 Bet Slider: Drag to choose your raise amount (min = currentBet + 1).\n" +
                "\u2022 Pot Display: Shows how many chips are in the pot.\n" +
                "\u2022 Chips Display: Shows your remaining chips and the dealer's.\n" +
                "\u2022 Community Cards: Revealed in stages (Flop -> Turn -> River).\n" +
                "\u2022 Game Status: Text below the pot shows whose turn it is and outcomes.\n" +
                "\u2022 Exit (In-Game): Abandon hand, return to main menu.\n" +
                "\u2022 Exit (Main Menu): Quit the application.\n";

        TextArea ta = new TextArea(tutorialText);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(12);
        ta.setPrefColumnCount(50);
        ta.setStyle("-fx-control-inner-background: #1E352A; -fx-text-fill: white; -fx-font-size: 16;");

        Button back = styledButton("Back", "#f44336", e -> showHome());
        tutorialScreen.getChildren().setAll(h, ta, back);
    }

    private void setupGameScreen() {
        deck = new Deck();
        player = new Player("You", 1000);
        dealer = new Player("Dealer", 1000);
        ai = new PokerAI();

        gameScreen = new BorderPane();
        gameScreen.setStyle("-fx-background-color: #2B4C1E;");
        gameScreen.setPadding(new Insets(30));

        // Top: Dealer
        dealerCards = new HBox(10);
        dealerCards.setAlignment(Pos.CENTER);
        dealerChips = new Label("Dealer: 1000");
        styleLabel(dealerChips);
        VBox top = new VBox(10, dealerCards, dealerChips);
        top.setAlignment(Pos.CENTER);

        // Center: Community + Status
        communityCards = new HBox(10);
        communityCards.setAlignment(Pos.CENTER);
        potLabel = new Label("Pot: 0");
        styleLabel(potLabel);
        gameStatus = new Label("Welcome!");
        styleLabel(gameStatus, 24);
        VBox center = new VBox(10, communityCards, potLabel, gameStatus);
        center.setAlignment(Pos.CENTER);

        // Bottom: Player + Controls
        playerCards = new HBox(10);
        playerCards.setAlignment(Pos.CENTER);
        playerChips = new Label("You: 1000");
        styleLabel(playerChips);

        betSlider = new Slider(1, 100, 1);
        betSlider.setShowTickLabels(true);
        betSlider.setShowTickMarks(true);
        betSlider.setMajorTickUnit(20);
        betSlider.setBlockIncrement(1);
        Label betLabel = new Label("Bet:");
        styleLabel(betLabel, 16);
        Label valLabel = new Label("1");
        styleLabel(valLabel, 16);
        betSlider.valueProperty().addListener((o, oldV, newV) -> {
            valLabel.setText(String.valueOf(newV.intValue()));
        });
        HBox betBox = new HBox(10, betLabel, betSlider, valLabel);
        betBox.setAlignment(Pos.CENTER);

        dealButton = styledButton("Deal", "#4CAF50", e -> startHand());
        foldButton = styledButton("Fold", "#f44336", e -> doFold());
        callButton = styledButton("Call", "#2196F3", e -> doCall());
        raiseButton = styledButton("Raise", "#FF9800", e -> doRaise());
        exitGameButton = styledButton("Exit", "#E53935", e -> {
            // mid‐game exit returns to main menu
            resetGame();
            showHome();
        });
        HBox btnBox = new HBox(15, dealButton, foldButton, callButton, raiseButton, exitGameButton);
        btnBox.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, playerCards, playerChips, betBox, btnBox);
        bottom.setAlignment(Pos.CENTER);

        gameScreen.setTop(top);
        gameScreen.setCenter(center);
        gameScreen.setBottom(bottom);
    }

    private Button styledButton(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(text);
        b.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, 18));
        b.setStyle(
                "-fx-background-color:" + color + "; -fx-text-fill:white; -fx-padding:8 20; -fx-background-radius:5;");
        b.setOnAction(h);
        return b;
    }

    private void styleLabel(Label l) {
        styleLabel(l, 22);
    }

    private void styleLabel(Label l, int size) {
        l.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, size));
        l.setTextFill(Color.WHITE);
    }

    private void showHome() {
        root.getChildren().setAll(homeScreen);
    }

    private void showRules() {
        root.getChildren().setAll(rulesScreen);
    }

    private void showTutorial() {
        root.getChildren().setAll(tutorialScreen);
    }

    private void showGame() {
        root.getChildren().setAll(gameScreen);
        startHand();
    }

    // Reset state so new hand truly starts fresh
    private void resetGame() {
        // restore everyone to full 1000
        player.setChips(1000);
        dealer.setChips(1000);

        // clear any old hands
        player.getHand().clear();
        dealer.getHand().clear();
        communityList.clear();

        // reset turn/dealer flags & stage
        // always start new match with the human as dealer:
        isPlayerDealer = true;

        isPlayerTurn = true;
        stage = StageName.PRE_FLOP;
        pot = callCount = 0;

        // clear displays
        communityCards.getChildren().clear();
        playerCards.getChildren().clear();
        dealerCards.getChildren().clear();
    }

    // Start a fresh hand
    private void startHand() {
        deck.shuffle();
        communityList.clear();
        communityCards.getChildren().clear();
        playerCards.getChildren().clear();
        dealerCards.getChildren().clear();

        isPlayerDealer = !isPlayerDealer;
        stage = StageName.PRE_FLOP;
        pot = callCount = 0;

        player.getHand().clear();
        dealer.getHand().clear();
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
        gameStatus.setText((isPlayerDealer ? "Dealer" : "You") + " posted blinds. Pre-flop.");
        updateUI();
        if (!isPlayerTurn)
            aiTurn();
    }

    private void doFold() {
        if (!isPlayerTurn || stage == StageName.SHOWDOWN)
            return;
        dealer.addChips(pot);
        gameStatus.setText("You folded. Dealer wins.");
        finishShowdown();
    }

    private void doCall() {
        if (!isPlayerTurn || stage == StageName.SHOWDOWN)
            return;
        if (!player.removeChips(currentBet)) {
            gameStatus.setText("Not enough chips to call.");
            return;
        }
        pot += currentBet;
        callCount++;
        gameStatus.setText("You called " + currentBet);
        updateUI();
        if (callCount >= 2)
            nextStage();
        else {
            isPlayerTurn = false;
            aiTurn();
        }
    }

    private void doRaise() {
        if (!isPlayerTurn || stage == StageName.SHOWDOWN)
            return;
        int amt = (int) betSlider.getValue();
        if (amt <= currentBet) {
            gameStatus.setText("Raise must exceed " + currentBet);
            return;
        }
        if (!player.removeChips(amt)) {
            gameStatus.setText("Not enough chips to raise.");
            return;
        }
        currentBet = amt;
        pot += amt;
        callCount = 1;
        gameStatus.setText("You raised to " + amt);
        updateUI();
        isPlayerTurn = false;
        aiTurn();
    }

    private void nextStage() {
        // Reveal cards & advance stages; river jumps straight to showdown
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
                // Immediately go to showdown
                stage = StageName.SHOWDOWN;
                finishShowdown();
                return;
            default:
                return;
        }
        callCount = 0;
        currentBet = 0;
        isPlayerTurn = !isPlayerDealer;
        updateUI();
        if (!isPlayerTurn)
            aiTurn();
        gameStatus.setText(stage.name() + " - " + (isPlayerTurn ? "Your turn" : "Dealer's turn"));
    }

    private void aiTurn() {
        disableActions(true);
        PauseTransition p = new PauseTransition(Duration.seconds(1));
        p.setOnFinished(e -> {
            PokerAI.Action act = ai.decideAction(dealer.getHand(), visibleCommunity(), currentBet, pot);
            switch (act) {
                case FOLD:
                    player.addChips(pot);
                    gameStatus.setText("Dealer folded. You win!");
                    finishShowdown();
                    break;
                case CALL:
                    dealer.removeChips(currentBet);
                    pot += currentBet;
                    callCount++;
                    gameStatus.setText("Dealer called.");
                    updateUI();
                    if (callCount >= 2)
                        nextStage();
                    else {
                        isPlayerTurn = true;
                        disableActions(false);
                    }
                    break;
                case RAISE:
                    int r = ai.decideRaiseAmount(currentBet, pot, dealer.getChips());
                    dealer.removeChips(r);
                    pot += r;
                    currentBet = r;
                    callCount = 1;
                    gameStatus.setText("Dealer raised to " + r);
                    updateUI();
                    isPlayerTurn = true;
                    disableActions(false);
                    break;
            }
        });
        p.play();
    }

    private List<Card> visibleCommunity() {
        List<Card> v = new ArrayList<>();
        for (Card c : communityList)
            if (c.isFaceUp())
                v.add(c);
        return v;
    }

    private void finishShowdown() {
        // reveal all
        dealer.getHand().forEach(c -> c.setFaceUp(true));
        communityList.forEach(c -> c.setFaceUp(true));
        updateUI();
        evaluateWinner();
        showEndDialog();
    }

    private void evaluateWinner() {
        List<Card> pFull = new ArrayList<>(player.getHand());
        pFull.addAll(communityList);
        List<Card> dFull = new ArrayList<>(dealer.getHand());
        dFull.addAll(communityList);

        HandRank hp = HandRank.evaluate(pFull);
        HandRank hd = HandRank.evaluate(dFull);

        if (hp.value > hd.value) {
            player.addChips(pot);
            gameStatus.setText("You win with " + hp.name + "!");
        } else if (hd.value > hp.value) {
            dealer.addChips(pot);
            gameStatus.setText("Dealer wins with " + hd.name + "!");
        } else {
            gameStatus.setText("Tie: " + hp.name + "!");
        }
        pot = 0;
        updateUI();
    }

    private void showEndDialog() {
        // create a modal dialog Stage
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Hand Over");

        // layout
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #2B4C1E; -fx-border-color: #FFD700; -fx-border-width: 2;");

        // result message
        Label msg = new Label(gameStatus.getText());
        msg.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, 24));
        msg.setTextFill(Color.WHITE);

        // Play Again button
        Button playAgain = styledButton("Play Again", "#4CAF50", e -> {
            dialog.close();
            resetGame(); // reset chips & state
            startHand(); // deal fresh hand
        });

        // Main Menu button
        Button menu = styledButton("Main Menu", "#1976D2", e -> {
            dialog.close();
            resetGame();
            showHome();
        });

        HBox btns = new HBox(15, playAgain, menu);
        btns.setAlignment(Pos.CENTER);

        box.getChildren().addAll(msg, btns);

        Scene sdScene = new Scene(box);
        dialog.setScene(sdScene);
        dialog.showAndWait();
    }

    private void updateUI() {
        // cards
        playerCards.getChildren().clear();
        dealerCards.getChildren().clear();
        communityCards.getChildren().clear();
        for (Card c : player.getHand())
            playerCards.getChildren().add(c.getCardView());
        for (Card c : dealer.getHand()) {
            c.setFaceUp(stage == StageName.SHOWDOWN);
            dealerCards.getChildren().add(c.getCardView());
        }
        for (Card c : communityList)
            communityCards.getChildren().add(c.getCardView());

        // labels
        playerChips.setText("You:    " + player.getChips());
        dealerChips.setText("Dealer: " + dealer.getChips());
        potLabel.setText("Pot:    " + pot);

        // slider bounds: min = currentBet+1
        int min = currentBet + 1;
        int max = Math.max(min, Math.min(player.getChips(), dealer.getChips()));
        betSlider.setMin(min);
        betSlider.setMax(max);
        if (betSlider.getValue() < min)
            betSlider.setValue(min);

        // buttons
        dealButton.setDisable(stage != StageName.SHOWDOWN);
        disableActions(!isPlayerTurn || stage == StageName.SHOWDOWN);
    }

    private void disableActions(boolean d) {
        foldButton.setDisable(d);
        callButton.setDisable(d);
        raiseButton.setDisable(d || betSlider.getMax() < betSlider.getMin());
        betSlider.setDisable(d);
    }

    /***** Helper Rank class *****/
    private static class HandRank {
        final String name;
        final int value;

        private HandRank(String n, int v) {
            name = n;
            value = v;
        }

        static HandRank evaluate(List<Card> cards) {
            Map<Integer, Integer> cnt = new HashMap<>();
            for (Card c : cards)
                cnt.merge(c.getValue(), 1, Integer::sum);
            boolean trips = cnt.values().stream().anyMatch(x -> x >= 3);
            long pairs = cnt.values().stream().filter(x -> x >= 2).count();
            if (trips)
                return new HandRank("Three of a Kind", 4);
            if (pairs >= 2)
                return new HandRank("Two Pair", 3);
            if (pairs == 1)
                return new HandRank("Pair", 2);
            int high = cnt.keySet().stream().mapToInt(i -> i).max().orElse(0);
            return new HandRank("High Card", 1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
