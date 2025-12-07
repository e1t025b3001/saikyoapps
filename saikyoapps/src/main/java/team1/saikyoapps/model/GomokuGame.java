package team1.saikyoapps.model;

public class GomokuGame {
  String gameId;
  String playerBlack;
  String playerWhite;
  String boardState; // JSON
  String turn;
  String status; // playing / finished

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public String getPlayerBlack() {
    return playerBlack;
  }

  public void setPlayerBlack(String playerBlack) {
    this.playerBlack = playerBlack;
  }

  public String getPlayerWhite() {
    return playerWhite;
  }

  public void setPlayerWhite(String playerWhite) {
    this.playerWhite = playerWhite;
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
