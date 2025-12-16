package team1.saikyoapps.model;

import java.time.Instant;
import java.util.Arrays;

/**
 * マッチ情報（簡易版）
 */
public class Match {
  private String matchId;
  private String game;
  private String player1;
  private String player2;
  private int[] board = new int[9]; // 0=空,1=player1(X),2=player2(O)
  private String turn; // ユーザ名のターン保持
  private String status; // waiting|playing|finished
  private String winner; // 勝者のユーザ名
  private long createdAt = Instant.now().toEpochMilli();

  public Match() {
  }

  public Match(String matchId, String game, String player1, String player2) {
    this.matchId = matchId;
    this.game = game;
    this.player1 = player1;
    this.player2 = player2;
    Arrays.fill(this.board, 0);
    this.turn = player1;
    this.status = "playing";
    this.winner = null;
  }

  // getters / setters
  public String getMatchId() {
    return matchId;
  }

  public void setMatchId(String matchId) {
    this.matchId = matchId;
  }

  public String getGame() {
    return game;
  }

  public void setGame(String game) {
    this.game = game;
  }

  public String getPlayer1() {
    return player1;
  }

  public void setPlayer1(String player1) {
    this.player1 = player1;
  }

  public String getPlayer2() {
    return player2;
  }

  public void setPlayer2(String player2) {
    this.player2 = player2;
  }

  public int[] getBoard() {
    return board;
  }

  public void setBoard(int[] board) {
    this.board = board;
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

  public String getWinner() {
    return winner;
  }

  public void setWinner(String winner) {
    this.winner = winner;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
}
