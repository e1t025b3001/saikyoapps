package team1.saikyoapps.darour.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import team1.saikyoapps.darour.model.Combination;
import team1.saikyoapps.darour.model.DarourGame;
import team1.saikyoapps.darour.model.DarourGameMapper;
import team1.saikyoapps.darour.model.DarourGameState;
import team1.saikyoapps.darour.model.DarourGameStateMapper;

@Service
public class DarourPlayTurnService {
  @Autowired
  DarourGameMapper darourGameMapper;

  @Autowired
  DarourGameStateMapper darourGameStateMapper;

  public boolean play(String gameID, String playerName, Combination playCombination) {
    // ゲーム状態を取得
    DarourGameState gameState = darourGameStateMapper.selectDarourGameStateByGameID(gameID);

    DarourGame game = darourGameMapper.selectDarourGameByPlayer(playerName);

    Integer playerNumber = game.getPlayer1().equals(playerName) ? 0
        : game.getPlayer2().equals(playerName) ? 1
            : 2;

    // プレイヤーの手番か確認
    if (!gameState.getCurrentPlayerIndex().equals(playerNumber)) {
      return false; // 不正な手番
    }

    // Combinationが有効か確認
    if (!Combination.isValidCombination(playCombination.cards)) {
      return false; // 無効な組み合わせ
    }

    // 場の組み合わせと比較して有効か確認
    Combination tableCombination = gameState.getTableCombination();
    if (tableCombination != null) {
      // 場にカードがある場合、同じ枚数かつより強い組み合わせでなければならない
      if (playCombination.cards.size() != tableCombination.cards.size()) {
        return false; // 枚数が異なる
      }
      if (playCombination.compareTo(tableCombination) <= 0) {
        return false; // 場の組み合わせより弱い
      }
    }
    // 場が空の場合（新しいラウンド）、有効な組み合わせなら何でも出せる

    var playerHand = playerNumber == 0
        ? gameState.getPlayer1Hand()
        : playerNumber == 1
            ? gameState.getPlayer2Hand()
            : gameState.getPlayer3Hand();

    // 手札からカードを削除
    if (!playerHand.removeCards(playCombination)) {
      return false; // 手札にないカードを出そうとした場合
    }

    // 場の組み合わせを更新
    gameState.setTableCombination(playCombination);

    // 最後にプレイしたプレイヤーを記録
    gameState.setLastPlayedPlayerIndex(playerNumber);

    // 次のプレイヤーに手番を移す
    gameState.setCurrentPlayerIndex((gameState.getCurrentPlayerIndex() + 1) % 3);

    // ゲーム状態を保存
    darourGameStateMapper.updateDarourGameState(gameState);

    return true;
  }

  public boolean pass(String gameID, String playerName) {
    // ゲーム状態を取得
    DarourGameState gameState = darourGameStateMapper.selectDarourGameStateByGameID(gameID);
    if (gameState == null) {
      return false;
    }

    DarourGame game = darourGameMapper.selectDarourGameByPlayer(playerName);
    if (game == null) {
      return false;
    }

    Integer playerNumber = game.getPlayer1().equals(playerName) ? 0
        : game.getPlayer2().equals(playerName) ? 1
            : 2;

    // プレイヤーの手番か確認
    if (!gameState.getCurrentPlayerIndex().equals(playerNumber)) {
      return false; // 不正な手番
    }

    // 次のプレイヤーに手番を移す
    int nextPlayer = (gameState.getCurrentPlayerIndex() + 1) % 3;
    gameState.setCurrentPlayerIndex(nextPlayer);

    // 全員がパスした場合（最後にプレイしたプレイヤーに戻った場合）、場をリセット
    if (gameState.getLastPlayedPlayerIndex() != null && nextPlayer == gameState.getLastPlayedPlayerIndex()) {
      gameState.setTableCombination(null);
      gameState.setLastPlayedPlayerIndex(null);
    }

    // ゲーム状態を保存
    darourGameStateMapper.updateDarourGameState(gameState);

    return true;
  }
}
