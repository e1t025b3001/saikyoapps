package team1.saikyoapps.darour.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.springframework.stereotype.Service;

import team1.saikyoapps.darour.model.Card;
import team1.saikyoapps.darour.model.DarourGameState;
import team1.saikyoapps.darour.model.Hand;
import team1.saikyoapps.darour.model.Rank;
import team1.saikyoapps.darour.model.Suit;

/**
 * ゲーム開始時の配牌・初期化を担当するサービス。
 */
@Service
public class InitializeDarourGameService {
  private final Random random;

  // テストで差し替え可能にするためコンストラクタ注入
  public InitializeDarourGameService() {
    this(new Random());
  }

  public InitializeDarourGameService(Random random) {
    this.random = random;
  }

  public DarourGameState createDarourGameState(String gameID) {
    DarourGameState result = new DarourGameState();

    ArrayList<Hand> hands = dealInitialHands();

    result.setPlayer1Hand(hands.get(0));
    result.setPlayer2Hand(hands.get(1));
    result.setPlayer3Hand(hands.get(2));

    // 手札が1枚少ないプレイヤー（クラブの3を持っていたプレイヤー）から開始
    result.setCurrentPlayerIndex(findStartingPlayerIndex(hands));

    result.setGameID(gameID);
    result.setLastPlayedPlayerIndex(null);
    result.setTableCombination(null);

    return result;
  }

  private Integer findStartingPlayerIndex(ArrayList<Hand> hands) {
    // 手札が1枚少ないプレイヤーを探す

    int minIndex = 0;
    int minSize = hands.get(0).getCards().size();

    for (int i = 1; i < hands.size(); i++) {
      int size = hands.get(i).getCards().size();
      if (size < minSize) {
        minSize = size;
        minIndex = i;
      }
    }

    return minIndex;
  }

  private ArrayList<Hand> dealInitialHands() {
    final int NUM_PLAYERS = 3;

    // 山札を作成
    ArrayList<Card> deck = new ArrayList<>();
    for (Suit suit : Suit.values()) {
      for (Rank rank : Rank.values()) {
        deck.add(Card.of(suit, rank));
      }
    }

    // シャッフル
    Collections.shuffle(deck, random);

    // 各プレイヤーに手札を配る
    ArrayList<Hand> hands = new ArrayList<>(); // プレイヤー数は3人固定
    int cardsPerPlayer = deck.size() / NUM_PLAYERS;
    for (int i = 0; i < NUM_PLAYERS; i++) {
      ArrayList<Card> playerCards = new ArrayList<>(
          deck.subList(i * cardsPerPlayer, (i + 1) * cardsPerPlayer));
      hands.add(new Hand(playerCards));
    }

    // 手札から，クラブの3を削除する
    for (Hand hand : hands) {
      hand.getCards().removeIf(card -> card.getSuit() == Suit.CLUB && card.getRank() == Rank.THREE);
    }

    // ソート
    for (Hand hand : hands) {
      hand.sortHand();
    }

    return hands;
  }
}
