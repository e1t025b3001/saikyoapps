package team1.saikyoapps.darour.model;

public enum Suit {
  // 強さ: ♣ < ♦ < ♥ < ♠
  CLUB("♣", Color.BLACK, 0),
  DIAMOND("♦", Color.RED, 1),
  HEART("♥", Color.RED, 2),
  SPADE("♠", Color.BLACK, 3);

  public enum Color {
    RED, BLACK
  }

  private final String suit;
  private final Color color;
  private final int strength;

  public int getStrength() {
    return strength;
  }

  Suit(String suit, Color color, int strength) {
    this.suit = suit;
    this.color = color;
    this.strength = strength;
  }

  public boolean isRed() {
    return color == Color.RED;
  }

  public boolean isBlack() {
    return color == Color.BLACK;
  }

  public boolean isStrongerThan(Suit other) {
    return other.strength < this.strength;
  }

  @Override
  public String toString() {
    return suit;
  }

  // DB保存用のシリアライズ
  public String serialize() {
    return suit;
  }

  // DB読み込み用のデシリアライズ
  public static Suit deserialize(String value) {
    for (Suit suit : Suit.values()) {
      if (suit.suit.equals(value)) {
        return suit;
      }
    }

    throw new IllegalArgumentException("不正なスート: " + value);
  }
}
