package team1.saikyoapps.darour.model;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Hand {
  private final ArrayList<Card> cards;

  public Hand(ArrayList<Card> cards) {
    this.cards = cards;
  }

  public void sortHand() {
    // 弱いほど前に来るようにソート
    cards.sort((card1, card2) -> {
      if (card1.getRank() != card2.getRank()) {
        return card1.getRank().isStrongerThan(card2.getRank()) ? 1 : -1;
      }

      return card1.getSuit().isStrongerThan(card2.getSuit()) ? 1 : -1;
    });
  }

  public boolean removeCards(Combination combination) {
    for (Card card : combination.cards) {
      if (!cards.remove(card)) {
        // 手札にないカードを出そうとした場合
        return false;
      }
    }

    return true;
  }

  public boolean hasClubThree() {
    for (Card card : cards) {
      if (card.getSuit() == Suit.CLUB && card.getRank() == Rank.THREE) {
        return true;
      }
    }
    return false;
  }

  public ArrayList<Card> getCards() {
    return cards;
  }

  @Override
  public String toString() {
    StringBuilder handString = new StringBuilder("Hand: ");

    for (Card card : cards) {
      handString.append(card.toString()).append(" ");
    }

    return handString.toString().trim();
  }

  // DB保存用のシリアライズ
  public String serialize() {
    return cards.stream()
        .map(Card::serialize)
        .collect(Collectors.joining(","));
  }

  // DB読み込み用のデシリアライズ
  public static Hand deserialize(String value) {
    if (value == null || value.isEmpty()) {
      return new Hand(new ArrayList<>());
    }

    String[] cardStrings = value.split(",");
    ArrayList<Card> cards = new ArrayList<>();
    for (String cardString : cardStrings) {
      cards.add(Card.deserialize(cardString));
    }
    return new Hand(cards);
  }

}
