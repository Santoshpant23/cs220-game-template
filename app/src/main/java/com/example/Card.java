package com.example;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class Card {
    private final String suit;
    private final String rank;
    private final int value;
    private boolean faceUp;
    private StackPane cardView;

    public Card(String suit, String rank, int value) {
        this.suit = suit;
        this.rank = rank;
        this.value = value;
        this.faceUp = true;
        loadCardView();
    }

    private String getAssetFileName() {
        String rankCode = rank;
        if (rank.equals("10"))
            rankCode = "10";
        else if (rank.equals("J"))
            rankCode = "J";
        else if (rank.equals("Q"))
            rankCode = "Q";
        else if (rank.equals("K"))
            rankCode = "K";
        else if (rank.equals("A"))
            rankCode = "A";
        // 2-9 are already correct
        String suitCode = suit.substring(0, 1).toUpperCase();
        return "/assets/" + rankCode + suitCode + ".png";
    }

    private void loadCardView() {
        cardView = new StackPane();
        String imagePath = getAssetFileName();
        try {
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            if (image.isError() || image.getWidth() == 0)
                throw new Exception();
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(100);
            imageView.setFitHeight(140);
            cardView.getChildren().add(imageView);
        } catch (Exception e) {
            Label label = new Label(rank + "\n" + suit);
            label.setStyle(
                    "-fx-background-color: white; -fx-border-color: black; -fx-padding: 10; -fx-font-size: 16px; -fx-alignment: center;");
            label.setMinSize(100, 140);
            cardView.getChildren().add(label);
        }
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
        cardView.getChildren().clear();
        if (!faceUp) {
            try {
                Image backImage = new Image(getClass().getResourceAsStream("/assets/Back.png"));
                if (backImage.isError() || backImage.getWidth() == 0)
                    throw new Exception();
                ImageView imageView = new ImageView(backImage);
                imageView.setFitWidth(100);
                imageView.setFitHeight(140);
                cardView.getChildren().add(imageView);
            } catch (Exception e) {
                Label label = new Label("Card\nBack");
                label.setStyle(
                        "-fx-background-color: gray; -fx-border-color: black; -fx-padding: 10; -fx-font-size: 16px; -fx-alignment: center;");
                label.setMinSize(100, 140);
                cardView.getChildren().add(label);
            }
        } else {
            loadCardView();
        }
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public StackPane getCardView() {
        return cardView;
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}