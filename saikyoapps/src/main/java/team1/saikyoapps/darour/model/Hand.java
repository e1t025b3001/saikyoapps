package team1.saikyoapps.darour.model;

import java.util.ArrayList;

public class Hand {
  public static final Integer HAND_SIZE = 17;

  private final ArrayList<Card> cards;

  public Hand(ArrayList<Card> cards) {
    if (cards.size() != HAND_SIZE) {
      throw new IllegalArgumentException("A hand must contain exactly " + HAND_SIZE + " cards.");
    }

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
}
