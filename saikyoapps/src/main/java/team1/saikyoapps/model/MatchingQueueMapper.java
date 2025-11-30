package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MatchingQueueMapper {
  @Select("SELECT COUNT(*) FROM matching_queue WHERE status = 'WAITING' AND requested_game = #{game}")
  int countWaitingPlayersByGame(String game);

  @Insert("INSERT INTO matching_queue (user_name, requested_game, status) VALUES (#{userName}, #{requestedGame}, #{status})")
  void insert(String userName, String requestedGame, String status);

  // 取得該遊戲所有等待中的記錄（依 id 由小到大）
  @Select("SELECT id, user_name AS userName, requested_game AS requestedGame, status FROM matching_queue WHERE status = 'WAITING' AND requested_game = #{game} ORDER BY id")
  List<MatchingQueue> findWaitingByGame(String game);

  // 取得前 N 位等待者
  @Select("SELECT id, user_name AS userName, requested_game AS requestedGame, status FROM matching_queue WHERE status = 'WAITING' AND requested_game = #{game} ORDER BY id LIMIT #{limit}")
  List<MatchingQueue> findFirstNWaitingByGame(String game, int limit);

  // 用 id 更新狀態
  @Update("UPDATE matching_queue SET status = #{status} WHERE id = #{id}")
  void updateStatusById(int id, String status);

  // 刪除指定使用者在指定遊戲的所有等待紀錄（結束比賽時用）
  @Delete("DELETE FROM matching_queue WHERE user_name = #{userName} AND requested_game = #{game}")
  void deleteByUserAndGame(String userName, String game);

  // 檢查該使用者是否已有正在等待或已配對的紀錄
  @Select("SELECT COUNT(*) FROM matching_queue WHERE user_name = #{userName} AND status IN ('WAITING','MATCHED')")
  int countActiveByUser(String userName);

  // 取得目前正在 playing 的玩家（從 players_status 表）
  @Select("SELECT user_name FROM players_status WHERE status = 'playing' AND current_game = #{game}")
  List<String> findPlayingUsersByGame(String game);

  // players_status 操作
  @Select("SELECT user_name AS userName, status, current_game AS currentGame FROM players_status WHERE user_name = #{userName}")
  PlayerStatus findPlayerStatus(String userName);

  @Insert("INSERT INTO players_status (user_name, status, current_game) VALUES (#{userName}, #{status}, #{currentGame})")
  void insertPlayerStatus(String userName, String status, String currentGame);

  @Update("UPDATE players_status SET status = #{status}, current_game = #{currentGame} WHERE user_name = #{userName}")
  void updatePlayerStatus(String userName, String status, String currentGame);

  @Delete("DELETE FROM players_status WHERE user_name = #{userName}")
  void deletePlayerStatus(String userName);
}
