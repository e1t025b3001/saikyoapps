package team1.saikyoapps.darour.model;

public enum Card {
  CLUB_THREE(Suit.CLUB, Rank.THREE),
  CLUB_FOUR(Suit.CLUB, Rank.FOUR),
  CLUB_FIVE(Suit.CLUB, Rank.FIVE),
  CLUB_SIX(Suit.CLUB, Rank.SIX),
  CLUB_SEVEN(Suit.CLUB, Rank.SEVEN),
  CLUB_EIGHT(Suit.CLUB, Rank.EIGHT),
  CLUB_NINE(Suit.CLUB, Rank.NINE),
  CLUB_TEN(Suit.CLUB, Rank.TEN),
  CLUB_JACK(Suit.CLUB, Rank.JACK),
  CLUB_QUEEN(Suit.CLUB, Rank.QUEEN),
  CLUB_KING(Suit.CLUB, Rank.KING),
  CLUB_ACE(Suit.CLUB, Rank.ACE),
  CLUB_TWO(Suit.CLUB, Rank.TWO),
  DIAMOND_THREE(Suit.DIAMOND, Rank.THREE),
  DIAMOND_FOUR(Suit.DIAMOND, Rank.FOUR),
  DIAMOND_FIVE(Suit.DIAMOND, Rank.FIVE),
  DIAMOND_SIX(Suit.DIAMOND, Rank.SIX),
  DIAMOND_SEVEN(Suit.DIAMOND, Rank.SEVEN),
  DIAMOND_EIGHT(Suit.DIAMOND, Rank.EIGHT),
  DIAMOND_NINE(Suit.DIAMOND, Rank.NINE),
  DIAMOND_TEN(Suit.DIAMOND, Rank.TEN),
  DIAMOND_JACK(Suit.DIAMOND, Rank.JACK),
  DIAMOND_QUEEN(Suit.DIAMOND, Rank.QUEEN),
  DIAMOND_KING(Suit.DIAMOND, Rank.KING),
  DIAMOND_ACE(Suit.DIAMOND, Rank.ACE),
  DIAMOND_TWO(Suit.DIAMOND, Rank.TWO),
  HEART_THREE(Suit.HEART, Rank.THREE),
  HEART_FOUR(Suit.HEART, Rank.FOUR),
  HEART_FIVE(Suit.HEART, Rank.FIVE),
  HEART_SIX(Suit.HEART, Rank.SIX),
  HEART_SEVEN(Suit.HEART, Rank.SEVEN),
  HEART_EIGHT(Suit.HEART, Rank.EIGHT),
  HEART_NINE(Suit.HEART, Rank.NINE),
  HEART_TEN(Suit.HEART, Rank.TEN),
  HEART_JACK(Suit.HEART, Rank.JACK),
  HEART_QUEEN(Suit.HEART, Rank.QUEEN),
  HEART_KING(Suit.HEART, Rank.KING),
  HEART_ACE(Suit.HEART, Rank.ACE),
  HEART_TWO(Suit.HEART, Rank.TWO),
  SPADE_THREE(Suit.SPADE, Rank.THREE),
  SPADE_FOUR(Suit.SPADE, Rank.FOUR),
  SPADE_FIVE(Suit.SPADE, Rank.FIVE),
  SPADE_SIX(Suit.SPADE, Rank.SIX),
  SPADE_SEVEN(Suit.SPADE, Rank.SEVEN),
  SPADE_EIGHT(Suit.SPADE, Rank.EIGHT),
  SPADE_NINE(Suit.SPADE, Rank.NINE),
  SPADE_TEN(Suit.SPADE, Rank.TEN),
  SPADE_JACK(Suit.SPADE, Rank.JACK),
  SPADE_QUEEN(Suit.SPADE, Rank.QUEEN),
  SPADE_KING(Suit.SPADE, Rank.KING),
  SPADE_ACE(Suit.SPADE, Rank.ACE),
  SPADE_TWO(Suit.SPADE, Rank.TWO);

  private final Suit suit;
  private final Rank rank;

  Card(Suit suit, Rank rank) {
    this.suit = suit;
    this.rank = rank;
  }

  public static Card of(Suit suit, Rank rank) {
    for (Card c : Card.values()) {
      if (c.suit == suit && c.rank == rank) {
        return c;
      }
    }
    throw new IllegalArgumentException("No Card for suit=" + suit + " rank=" + rank);
  }

  public Suit getSuit() {
    return suit;
  }

  public Rank getRank() {
    return rank;
  }

  public boolean isStrongerThan(Card other) {
    // まずランクで比較、同じランクならスートで比較
    if (this.rank != other.rank) {
      return this.rank.isStrongerThan(other.rank);
    }
    return this.suit.isStrongerThan(other.suit);
  }

  public int compareStrength(Card other) {
    // まずランクで比較
    int rankCompare = Integer.compare(this.rank.getStrength(), other.rank.getStrength());
    if (rankCompare != 0) {
      return rankCompare;
    }
    // 同じランクならスートで比較
    return Integer.compare(this.suit.getStrength(), other.suit.getStrength());
  }

  @Override
  public String toString() {
    return suit.toString() + rank.toString();
  }

  // DB保存用のシリアライズ
  public String serialize() {
    return suit.serialize() + rank.serialize();
  }

  // DB読み込み用のデシリアライズ
  public static Card deserialize(String value) {
    if (value.length() < 2) {
      throw new IllegalArgumentException("不正なカード: " + value);
    }

    String suitString = value.substring(0, 1);
    String rankString = value.substring(1);

    Suit suit = Suit.deserialize(suitString);
    Rank rank = Rank.deserialize(rankString);

    return Card.of(suit, rank);
  }
}
