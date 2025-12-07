package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GomokuGameMapper {
  @Insert("INSERT INTO gomoku_game (game_id, player_black, player_white, board_state, turn, status) VALUES (#{gameId}, #{playerBlack}, #{playerWhite}, #{boardState}, #{turn}, #{status})")
  void insert(String gameId, String playerBlack, String playerWhite, String boardState, String turn, String status);

  @Select("SELECT game_id AS gameId, player_black AS playerBlack, player_white AS playerWhite, board_state AS boardState, turn, status FROM gomoku_game WHERE game_id = #{gameId}")
  GomokuGame findByGameId(String gameId);

  @Update("UPDATE gomoku_game SET board_state = #{boardState}, turn = #{turn}, status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE game_id = #{gameId}")
  void update(String gameId, String boardState, String turn, String status);

  // 新增：依玩家查詢該玩家所屬的遊戲 session (player_black 或 player_white)
  @Select("SELECT game_id AS gameId, player_black AS playerBlack, player_white AS playerWhite, board_state AS boardState, turn, status FROM gomoku_game WHERE player_black = #{player} OR player_white = #{player} LIMIT 1")
  GomokuGame findByPlayer(String player);
}
