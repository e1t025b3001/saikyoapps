package team1.saikyoapps.darour.model;

public enum Rank {
  // 3 < 4 < 5 < 6 < 7 < 8 < 9 < 10 < J < Q < K < A < 2 の順で強い
  THREE,
  FOUR,
  FIVE,
  SIX,
  SEVEN,
  EIGHT,
  NINE,
  TEN,
  JACK,
  QUEEN,
  KING,
  ACE,
  TWO;

  public boolean isStrongerThan(Rank other) {
    return this.ordinal() > other.ordinal();
  }

  @Override
  public String toString() {
    switch (this) {
      case THREE:
        return "3";
      case FOUR:
        return "4";
      case FIVE:
        return "5";
      case SIX:
        return "6";
      case SEVEN:
        return "7";
      case EIGHT:
        return "8";
      case NINE:
        return "9";
      case TEN:
        return "10";
      case JACK:
        return "J";
      case QUEEN:
        return "Q";
      case KING:
        return "K";
      case ACE:
        return "A";
      case TWO:
        return "2";
      default:
        throw new IllegalArgumentException();
    }
  }
}
