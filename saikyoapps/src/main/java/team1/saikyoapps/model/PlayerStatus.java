package team1.saikyoapps.model;

public class PlayerStatus {
  String userName;
  String status; // lobby / matching / playing
  String currentGame;

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCurrentGame() {
    return currentGame;
  }

  public void setCurrentGame(String currentGame) {
    this.currentGame = currentGame;
  }
}
