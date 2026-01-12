package team1.saikyoapps.darour.model;

public enum Rank {
  // 強さ: 3 < 4 < 5 < 6 < 7 < 8 < 9 < 10 < J < Q < K < A < 2
  THREE(0),
  FOUR(1),
  FIVE(2),
  SIX(3),
  SEVEN(4),
  EIGHT(5),
  NINE(6),
  TEN(7),
  JACK(8),
  QUEEN(9),
  KING(10),
  ACE(11),
  TWO(12);

  private final int strength;

  public int getStrength() {
    return strength;
  }

  Rank(int strength) {
    this.strength = strength;
  }

  public boolean isStrongerThan(Rank other) {
    return other.strength < this.strength;
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
        return "T";
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

  // DB保存用のシリアライズ
  public String serialize() {
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
        return "T";
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

  // DB読み込み用のデシリアライズ
  public static Rank deserialize(String value) {
    return switch (value) {
      case "3" -> THREE;
      case "4" -> FOUR;
      case "5" -> FIVE;
      case "6" -> SIX;
      case "7" -> SEVEN;
      case "8" -> EIGHT;
      case "9" -> NINE;
      case "T" -> TEN;
      case "J" -> JACK;
      case "Q" -> QUEEN;
      case "K" -> KING;
      case "A" -> ACE;
      case "2" -> TWO;
      default -> throw new IllegalArgumentException("不正なランク: " + value);
    };
  }
}
