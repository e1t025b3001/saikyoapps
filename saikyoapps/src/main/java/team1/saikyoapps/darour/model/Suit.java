package team1.saikyoapps.darour.model;

public enum Suit {
  // ♠ > ♥ > ♦ > ♣ の順で強い
  SPADE,
  HEART,
  DIAMOND,
  CLUB;

  public boolean isRed() {
    return this == HEART || this == DIAMOND;
  }

  public boolean isBlack() {
    return this == SPADE || this == CLUB;
  }

  public boolean isStrongerThan(Suit other) {
    return this.ordinal() < other.ordinal();
  }

  @Override
  public String toString() {
    switch (this) {
      case SPADE:
        return "♠";
      case HEART:
        return "♥";
      case DIAMOND:
        return "♦";
      case CLUB:
        return "♣";
      default:
        throw new IllegalArgumentException();
    }
  }
}
