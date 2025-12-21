package team1.saikyoapps.model;

public class MarubatsuGame {
  String gameId;
  String playerX;
  String playerO;
  String boardState; // JSON
  String turn; // "X" or "O"
  String status; // playing / finished

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public String getPlayerX() {
    return playerX;
  }

  public void setPlayerX(String playerX) {
    this.playerX = playerX;
  }

  public String getPlayerO() {
    return playerO;
  }

  public void setPlayerO(String playerO) {
    this.playerO = playerO;
  }

  public String getBoardState() {
    return boardState;
  }

  public void setBoardState(String boardState) {
    this.boardState = boardState;
  }

  public String getTurn() {
    return turn;
  }

  public void setTurn(String turn) {
    this.turn = turn;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
