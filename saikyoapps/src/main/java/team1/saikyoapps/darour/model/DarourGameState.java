package team1.saikyoapps.darour.model;

public class DarourGameState {
  public String gameID;
  public Hand player1Hand;
  public Hand player2Hand;

  public String getGameID() {
    return gameID;
  }

  public void setGameID(String gameID) {
    this.gameID = gameID;
  }

  public Hand getPlayer1Hand() {
    return player1Hand;
  }

  public void setPlayer1Hand(Hand player1Hand) {
    this.player1Hand = player1Hand;
  }

  public Hand getPlayer2Hand() {
    return player2Hand;
  }

  public void setPlayer2Hand(Hand player2Hand) {
    this.player2Hand = player2Hand;
  }

  public Hand getPlayer3Hand() {
    return player3Hand;
  }

  public void setPlayer3Hand(Hand player3Hand) {
    this.player3Hand = player3Hand;
  }

  public Integer getCurrentPlayerIndex() {
    return currentPlayerIndex;
  }

  public void setCurrentPlayerIndex(Integer currentPlayerIndex) {
    this.currentPlayerIndex = currentPlayerIndex;
  }

  public Integer getLastPlayedPlayerIndex() {
    return lastPlayedPlayerIndex;
  }

  public void setLastPlayedPlayerIndex(Integer lastPlayedPlayerIndex) {
    this.lastPlayedPlayerIndex = lastPlayedPlayerIndex;
  }

  public Combination getTableCombination() {
    return tableCombination;
  }

  public void setTableCombination(Combination tableCombination) {
    this.tableCombination = tableCombination;
  }

  public Hand player3Hand;
  public Integer currentPlayerIndex;
  public Integer lastPlayedPlayerIndex;
  public Combination tableCombination;
}
