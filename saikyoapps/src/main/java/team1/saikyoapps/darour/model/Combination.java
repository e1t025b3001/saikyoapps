package team1.saikyoapps.darour.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Combination {
  public enum CombinationResult {
    HIGH_CARD,
    PAIR,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH;

    // DB保存用のシリアライズ
    public String serialize() {
      return this.toString();
    }

    // DB読み込み用のデシリアライズ
    public static CombinationResult deserialize(String value) {
      for (CombinationResult result : CombinationResult.values()) {
        if (result.toString().equals(value)) {
          return result;
        }
      }
      throw new IllegalArgumentException("Unknown combination result: " + value);
    }
  }

  public final ArrayList<Card> cards;
  public final CombinationResult result;

  private Combination(ArrayList<Card> cards) {
    this.cards = cards;
    this.result = evaluate(cards).orElseThrow();
  }

  public static Combination of(ArrayList<Card> cards) {
    if (!isValidCombination(cards)) {
      throw new IllegalArgumentException("Invalid combination of cards");
    }
    return new Combination(cards);
  }

  public static Optional<CombinationResult> evaluate(ArrayList<Card> cards) {
    return switch (cards.size()) {
      case 1 -> isHighCard(cards) ? Optional.of(CombinationResult.HIGH_CARD) : Optional.empty();
      case 2 -> isPair(cards) ? Optional.of(CombinationResult.PAIR) : Optional.empty();
      case 3 -> isThreeOfAKind(cards) ? Optional.of(CombinationResult.THREE_OF_A_KIND) : Optional.empty();
      case 5 -> {
        if (isStraightFlush(cards)) {
          yield Optional.of(CombinationResult.STRAIGHT_FLUSH);
        } else if (isFourOfAKind(cards)) {
          yield Optional.of(CombinationResult.FOUR_OF_A_KIND);
        } else if (isFullHouse(cards)) {
          yield Optional.of(CombinationResult.FULL_HOUSE);
        } else if (isFlush(cards)) {
          yield Optional.of(CombinationResult.FLUSH);
        } else if (isStraight(cards)) {
          yield Optional.of(CombinationResult.STRAIGHT);
        } else {
          yield Optional.empty();
        }
      }
      default -> Optional.empty();
    };
  }

  public static boolean isValidCombination(ArrayList<Card> cards) {
    return evaluate(cards).isPresent();
  }

  private static boolean isHighCard(ArrayList<Card> cards) {
    return cards.size() == 1;
  }

  private static boolean isPair(ArrayList<Card> cards) {
    if (cards.size() != 2)
      return false;

    return cards.get(0).getRank() == cards.get(1).getRank();
  }

  private static boolean isThreeOfAKind(ArrayList<Card> cards) {
    if (cards.size() != 3)
      return false;

    return cards.get(0).getRank() == cards.get(1).getRank()
        && cards.get(1).getRank() == cards.get(2).getRank();
  }

  private static boolean isFullHouse(ArrayList<Card> cards) {
    if (cards.size() != 5)
      return false;

    Collection<Long> counts = cards.stream()
        .collect(Collectors.groupingBy(
            Card::getRank,
            Collectors.counting()))
        .values();

    return counts.size() == 2 &&
        counts.contains(3L) &&
        counts.contains(2L);
  }

  private static boolean isStraight(ArrayList<Card> cards) {
    if (cards.size() != 5)
      return false;

    // ランクを strength 順にソート
    List<Rank> ranks = cards.stream()
        .map(Card::getRank)
        .distinct()
        .sorted(Comparator.comparingInt(Rank::getStrength))
        .toList();

    // 同一ランクがあれば即失敗
    if (ranks.size() != 5)
      return false;

    // 連続性チェック
    for (int i = 0; i < 4; i++) {
      if (ranks.get(i).getStrength() + 1 != ranks.get(i + 1).getStrength()) {
        return false;
      }
    }

    return true;
  }

  private static boolean isFlush(ArrayList<Card> cards) {
    if (cards.size() != 5)
      return false;

    return cards.stream().allMatch(card -> card.getSuit() == cards.get(0).getSuit());
  }

  private static boolean isFourOfAKind(List<Card> cards) {
    if (cards.size() != 5)
      return false;

    return cards.stream()
        .collect(Collectors.groupingBy(
            Card::getRank,
            Collectors.counting()))
        .values()
        .contains(4L);
  }

  private static boolean isStraightFlush(ArrayList<Card> cards) {
    if (cards.size() != 5)
      return false;

    return isStraight(cards) && isFlush(cards);
  }

  // DB保存用のシリアライズ
  public String serialize() {
    String cardStrings = cards.stream()
        .map(Card::serialize)
        .collect(Collectors.joining(","));
    return cardStrings + "|" + result.serialize();
  }

  // DB読み込み用のデシリアライズ
  public static Combination deserialize(String value) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("不正なコンビネーション: " + value);
    }
    String[] parts = value.split("\\|");
    if (parts.length != 2) {
      throw new IllegalArgumentException("不正なコンビネーション: " + value);
    }
    String cardPart = parts[0];
    String resultPart = parts[1];
    String[] cardStrings = cardPart.split(",");
    ArrayList<Card> cards = new ArrayList<>();
    for (String cardString : cardStrings) {
      cards.add(Card.deserialize(cardString));
    }
    CombinationResult result = CombinationResult.deserialize(resultPart);
    Combination combination = new Combination(cards);
    if (combination.result != result) {
      throw new IllegalArgumentException("コンビネーションの評価結果が一致しません: " + value);
    }

    return combination;
  }
}
