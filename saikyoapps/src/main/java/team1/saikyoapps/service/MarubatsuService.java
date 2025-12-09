package team1.saikyoapps.service;

import org.springframework.stereotype.Service;

@Service
public class MarubatsuService {
  // board は長さ9の int[] で、1=X, 2=O
  public boolean checkWin(int[] board, int val) {
    int[][] lines = { {0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6} };
    for (int[] ln : lines) {
      if (board[ln[0]] == val && board[ln[1]] == val && board[ln[2]] == val) return true;
    }
    return false;
  }

  public boolean isFull(int[] board) {
    for (int v : board) if (v == 0) return false;
    return true;
  }
}
