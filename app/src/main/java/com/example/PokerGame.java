package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.*;

public class PokerGame extends Application {
    private Deck deck;
    private Player player;
    private Player dealer;
    private PokerAI ai;
    private HBox playerCards;
    private HBox dealerCards;
    private HBox communityCards;
    private Label playerChips;
    private Label dealerChips;
    private Label gameStatus;
    private Label potLabel;
    private Button dealButton, foldButton, callButton, raiseButton, exitButton;
    private Slider betSlider;
    private int currentBet = 0;
    private int pot = 0;
    private boolean isPlayerTurn = true;
    private boolean isPlayerDealer = false;

    private enum GameStage {
        PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN
    }

    private GameStage stage = GameStage.PRE_FLOP;
    private List<Card> communityCardList = new ArrayList<>();
    private int callCount = 0;

    private StackPane rootPane;
    private Scene mainScene;
    private VBox homeScreen, rulesScreen, tutorialScreen, gameScreenContainer;
    private BorderPane gameScreen;
    private Font pokerFont;

    @Override
    public void start(Stage primaryStage) {
        loadFont();
        rootPane = new StackPane();
        mainScene = new Scene(rootPane, 1000, 800);

        setupHomeScreen();
        setupRulesScreen();
        setupTutorialScreen();
        setupGameScreen();

        showHomeScreen();

        primaryStage.setTitle("Poker Game");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/KC.png")));
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    private void loadFont() {
        try {
            pokerFont = Font.loadFont(getClass().getResourceAsStream("/assets/Play-Regular.ttf"), 24);
        } catch (Exception e) {
            pokerFont = null;
        }
        if (pokerFont == null) {
            pokerFont = Font.font("Serif", FontWeight.BOLD, 24);
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

        Button startBtn = makeButton("Start Game", e -> showGameScreen(), "#388E3C");
        Button rulesBtn = makeButton("Rules", e -> showRulesScreen(), "#1976D2");
        Button tutorialBtn = makeButton("Tutorial", e -> showTutorialScreen(), "#FFA000");

        homeScreen.getChildren().addAll(logo, title, startBtn, rulesBtn, tutorialBtn);
    }

    private void setupRulesScreen() {
        rulesScreen = new VBox(20);
        rulesScreen.setAlignment(Pos.CENTER);
        rulesScreen.setStyle("-fx-background-color: #2B4C1E;");
        rulesScreen.setPadding(new Insets(60));

        Label rulesTitle = new Label("Poker Rules");
        rulesTitle.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, 40));
        rulesTitle.setTextFill(Color.web("#FFD700"));
        rulesTitle.setStyle("-fx-effect: dropshadow(gaussian, black, 4, 0.5, 2, 2);");

        Label rulesText = new Label(
                "1. Each player is dealt 3 cards.\n" +
                        "2. 5 community cards revealed in stages: Flop (3), Turn (1), River (1).\n" +
                        "3. Players bet each round: Fold, Call, or Raise.\n" +
                        "4. Best 3-card hand wins at showdown.\n" +
                        "5. High card if no pairs or trips.\n" +
                        "6. Dealer's cards hidden until showdown.\n" +
                        "7. Enjoy responsibly!");
        rulesText.setFont(Font.font(pokerFont.getFamily(), FontWeight.NORMAL, 22));
        rulesText.setTextFill(Color.WHITE);
        rulesText.setWrapText(true);
        rulesText.setTextAlignment(TextAlignment.CENTER);
        rulesText.setMaxWidth(700);

        Button backBtn = makeButton("Back", e -> showHomeScreen(), "#f44336");
        rulesScreen.getChildren().addAll(rulesTitle, rulesText, backBtn);
    }

    private void setupTutorialScreen() {
        tutorialScreen = new VBox(20);
        tutorialScreen.setAlignment(Pos.CENTER_LEFT);
        tutorialScreen.setStyle("-fx-background-color: #2B4C1E;");
        tutorialScreen.setPadding(new Insets(40));

        Label tutTitle = new Label("Game Tutorial");
        tutTitle.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, 40));
        tutTitle.setTextFill(Color.web("#FFD700"));

        TextArea tutText = new TextArea(
                "Deal: Starts a new hand, shuffles, posts blinds, and deals cards.\n" +
                        "Fold: Forfeit the round and concede chips to opponent.\n" +
                        "Call: Match the current bet to stay in the hand.\n" +
                        "Raise: Increase the bet above the current amount.\n" +
                        "Bet Slider: Choose your raise amount within available chips.\n" +
                        "Pot Display: Shows total chips in the middle.\n" +
                        "Chips Display: Shows remaining chips for each player.\n" +
                        "Community Cards: Shared cards revealed each stage.\n" +
                        "Game Status: Displays current turn and outcomes.\n" +
                        "Exit: Closes the application.");
        tutText.setWrapText(true);
        tutText.setEditable(false);
        tutText.setStyle("-fx-font-size: 16px; -fx-control-inner-background: #1E352A; -fx-text-fill: white;");
        tutText.setPrefRowCount(12);
        tutText.setPrefColumnCount(50);

        Button backBtn = makeButton("Back", e -> showHomeScreen(), "#f44336");
        tutorialScreen.getChildren().addAll(tutTitle, tutText, backBtn);
    }

    private void setupGameScreen() {
        deck = new Deck();
        player = new Player("Player", 1000);
        dealer = new Player("Dealer", 1000);
        ai = new PokerAI();
        communityCardList.clear();

        gameScreen = new BorderPane();
        gameScreen.setStyle("-fx-background-color: #2B4C1E;");
        gameScreen.setPadding(new Insets(30));

        // Top: Dealer
        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        dealerCards = new HBox(10);
        dealerCards.setAlignment(Pos.CENTER);
        dealerChips = new Label("Dealer: 1000");
        styleLabel(dealerChips);
        topBox.getChildren().addAll(dealerCards, dealerChips);

        // Center: Community & Status
        VBox centerBox = new VBox(10);
        centerBox.setAlignment(Pos.CENTER);
        communityCards = new HBox(10);
        communityCards.setAlignment(Pos.CENTER);
        potLabel = new Label("Pot: 0");
        styleLabel(potLabel);
        gameStatus = new Label("Welcome!");
        styleLabel(gameStatus, 24);
        centerBox.getChildren().addAll(communityCards, potLabel, gameStatus);

        // Bottom: Player & Controls
        VBox bottomBox = new VBox(10);
        bottomBox.setAlignment(Pos.CENTER);
        playerCards = new HBox(10);
        playerCards.setAlignment(Pos.CENTER);
        playerChips = new Label("You: 1000");
        styleLabel(playerChips);

        HBox betBox = new HBox(10);
        betBox.setAlignment(Pos.CENTER);
        Label betLbl = new Label("Bet:");
        styleLabel(betLbl, 16);
        betSlider = new Slider(10, 100, 10);
        betSlider.setPrefWidth(200);
        betSlider.setShowTickLabels(true);
        betSlider.setShowTickMarks(true);
        betSlider.setMajorTickUnit(20);
        betSlider.setBlockIncrement(10);
        Label valLbl = new Label("10");
        styleLabel(valLbl, 16);
        betSlider.valueProperty().addListener((o, oldV, newV) -> valLbl.setText(String.valueOf(newV.intValue())));
        betBox.getChildren().addAll(betLbl, betSlider, valLbl);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);
        dealButton = makeButton("Deal", e -> dealNewHand(), "#4CAF50");
        foldButton = makeButton("Fold", e -> fold(), "#f44336");
        callButton = makeButton("Call", e -> call(), "#2196F3");
        raiseButton = makeButton("Raise", e -> raise(), "#FF9800");
        exitButton = makeButton("Exit", e -> Platform.exit(), "#E53935");
        btnBox.getChildren().addAll(dealButton, foldButton, callButton, raiseButton, exitButton);

        bottomBox.getChildren().addAll(playerCards, playerChips, betBox, btnBox);

        gameScreen.setTop(topBox);
        gameScreen.setCenter(centerBox);
        gameScreen.setBottom(bottomBox);
    }

    private void styleLabel(Label lbl) {
        styleLabel(lbl, 22);
    }

    private void styleLabel(Label lbl, int size) {
        lbl.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, size));
        lbl.setTextFill(Color.WHITE);
    }

    private Button makeButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler,
            String colorHex) {
        Button b = new Button(text);
        b.setFont(Font.font(pokerFont.getFamily(), FontWeight.BOLD, 18));
        b.setStyle("-fx-padding: 8 20; -fx-background-radius: 5; -fx-text-fill: white; -fx-background-color: "
                + colorHex + ";");
        b.setOnAction(handler);
        return b;
    }

    private void showHomeScreen() {
        rootPane.getChildren().setAll(homeScreen);
    }

    private void showRulesScreen() {
        rootPane.getChildren().setAll(rulesScreen);
    }

    private void showTutorialScreen() {
        rootPane.getChildren().setAll(tutorialScreen);
    }

    private void showGameScreen() {
        rootPane.getChildren().setAll(gameScreen);
        dealNewHand();
    }

    private void dealNewHand() {
        deck.shuffle();
        communityCardList.clear();
        communityCards.getChildren().clear();
        playerCards.getChildren().clear();
        dealerCards.getChildren().clear();
        isPlayerDealer = !isPlayerDealer;
        stage = GameStage.PRE_FLOP;
        pot = 0;
        callCount = 0;

        player.clearHand();
        dealer.clearHand();
        for (int i = 0; i < 3; i++) {
            player.addCard(deck.drawCard());
            dealer.addCard(deck.drawCard());
        }
        for (int i = 0; i < 5; i++) {
            Card c = deck.drawCard();
            c.setFaceUp(false);
            communityCardList.add(c);
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
        updateDisplay();
        if (!isPlayerTurn)
            processAITurn();
    }

    private void fold() {
        if (!isPlayerTurn || stage == GameStage.SHOWDOWN)
            return;
        dealer.addChips(pot);
        gameStatus.setText("You folded. Dealer wins.");
        finishShowdown();
    }

    private void call() {
        if (!isPlayerTurn || stage == GameStage.SHOWDOWN)
            return;
        if (player.removeChips(currentBet)) {
            pot += currentBet;
            callCount++;
            gameStatus.setText("You called " + currentBet);
            updateDisplay();
            if (callCount >= 2)
                nextStageWithDelay();
            else {
                isPlayerTurn = false;
                processAITurn();
            }
        } else {
            gameStatus.setText("Not enough chips to call.");
        }
    }

    private void raise() {
        if (!isPlayerTurn || stage == GameStage.SHOWDOWN)
            return;
        int amt = (int) betSlider.getValue();
        if (amt <= currentBet) {
            gameStatus.setText("Raise must exceed " + currentBet);
            return;
        }
        if (player.removeChips(amt)) {
            currentBet = amt;
            pot += amt;
            callCount = 1;
            gameStatus.setText("You raised to " + amt);
            updateDisplay();
            isPlayerTurn = false;
            processAITurn();
        } else {
            gameStatus.setText("Not enough chips to raise.");
        }
    }

    private void nextStageWithDelay() {
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> nextStage());
        pause.play();
    }

    private void nextStage() {
        if (stage == GameStage.SHOWDOWN)
            return;
        switch (stage) {
            case PRE_FLOP:
                communityCardList.get(0).setFaceUp(true);
                communityCardList.get(1).setFaceUp(true);
                communityCardList.get(2).setFaceUp(true);
                stage = GameStage.FLOP;
                break;
            case FLOP:
                communityCardList.get(3).setFaceUp(true);
                stage = GameStage.TURN;
                break;
            case TURN:
                communityCardList.get(4).setFaceUp(true);
                stage = GameStage.RIVER;
                break;
            case RIVER:
                stage = GameStage.SHOWDOWN;
                break;
            default:
                return;
        }
        potLabel.setText("Pot: " + pot);
        gameStatus.setText(stage.name() + ": " + (isPlayerTurn ? "Your turn" : "Dealer's turn"));
        callCount = 0;
        currentBet = 0;
        isPlayerTurn = !isPlayerDealer;
        updateDisplay();
        if (!isPlayerTurn && stage != GameStage.SHOWDOWN)
            processAITurn();
        if (stage == GameStage.SHOWDOWN)
            finishShowdown();
    }

    private void processAITurn() {
        if (stage == GameStage.SHOWDOWN)
            return;
        disableActions(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            PokerAI.Action action = ai.decideAction(dealer.getHand(), visibleCommunity(), currentBet, pot);
            switch (action) {
                case FOLD:
                    player.addChips(pot);
                    gameStatus.setText("Dealer folded. You win.");
                    finishShowdown();
                    break;
                case CALL:
                    dealer.removeChips(currentBet);
                    pot += currentBet;
                    callCount++;
                    gameStatus.setText("Dealer called.");
                    updateDisplay();
                    if (callCount >= 2)
                        nextStageWithDelay();
                    else {
                        isPlayerTurn = true;
                        disableActions(false);
                    }
                    break;
                case RAISE:
                    int raiseAmt = ai.decideRaiseAmount(currentBet, pot);
                    dealer.removeChips(raiseAmt);
                    pot += raiseAmt;
                    currentBet = raiseAmt;
                    callCount = 1;
                    gameStatus.setText("Dealer raised to " + raiseAmt);
                    updateDisplay();
                    isPlayerTurn = true;
                    disableActions(false);
                    break;
            }
        });
        pause.play();
    }

    private List<Card> visibleCommunity() {
        List<Card> visible = new ArrayList<>();
        for (Card c : communityCardList)
            if (c.isFaceUp())
                visible.add(c);
        return visible;
    }

    private void revealAll() {
        dealer.getHand().forEach(c -> c.setFaceUp(true));
        communityCardList.forEach(c -> c.setFaceUp(true));
        updateDisplay();
    }

    private void finishShowdown() {
        revealAll();
        evaluateWinner();
    }

    private void evaluateWinner() {
        List<Card> pFull = new ArrayList<>(player.getHand());
        pFull.addAll(communityCardList);
        List<Card> dFull = new ArrayList<>(dealer.getHand());
        dFull.addAll(communityCardList);
        HandRank pRank = evaluateRank(pFull);
        HandRank dRank = evaluateRank(dFull);
        if (pRank.value > dRank.value) {
            player.addChips(pot);
            gameStatus.setText("You win: " + pRank.name);
        } else if (dRank.value > pRank.value) {
            dealer.addChips(pot);
            gameStatus.setText("Dealer wins: " + dRank.name);
        } else {
            gameStatus.setText("Tie: " + pRank.name);
        }
        pot = 0;
        updateDisplay();
    }

    private HandRank evaluateRank(List<Card> cards) {
        Map<Integer, Integer> count = new HashMap<>();
        for (Card c : cards)
            count.merge(c.getValue(), 1, Integer::sum);
        if (count.values().stream().anyMatch(v -> v >= 3))
            return new HandRank("Three of a Kind", 4);
        long pairs = count.values().stream().filter(v -> v >= 2).count();
        if (pairs >= 2)
            return new HandRank("Two Pair", 3);
        if (pairs == 1)
            return new HandRank("Pair", 2);
        int high = count.keySet().stream().mapToInt(v -> v).max().orElse(0);
        return new HandRank("High Card", 1);
    }

    private void updateDisplay() {
        playerCards.getChildren().clear();
        dealerCards.getChildren().clear();
        communityCards.getChildren().clear();
        for (Card c : player.getHand())
            playerCards.getChildren().add(c.getCardView());
        for (Card c : dealer.getHand()) {
            c.setFaceUp(stage == GameStage.SHOWDOWN);
            dealerCards.getChildren().add(c.getCardView());
        }
        for (Card c : communityCardList)
            communityCards.getChildren().add(c.getCardView());
        playerChips.setText("You: " + player.getChips());
        dealerChips.setText("Dealer: " + dealer.getChips());
        potLabel.setText("Pot: " + pot);
        disableActions(stage == GameStage.SHOWDOWN || !isPlayerTurn);
        dealButton.setDisable(stage != GameStage.SHOWDOWN);
    }

    private void disableActions(boolean disable) {
        foldButton.setDisable(disable);
        callButton.setDisable(disable);
        raiseButton.setDisable(disable);
        betSlider.setDisable(disable);
    }

    private record HandRank(String name, int value) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
