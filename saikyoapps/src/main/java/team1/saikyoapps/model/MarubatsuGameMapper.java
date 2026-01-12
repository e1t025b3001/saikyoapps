package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MarubatsuGameMapper {
  @Insert("INSERT INTO marubatsu_game (game_id, player_x, player_o, board_state, turn, status) VALUES (#{gameId}, #{playerX}, #{playerO}, #{boardState}, #{turn}, #{status})")
  void insert(String gameId, String playerX, String playerO, String boardState, String turn, String status);

  @Select("SELECT game_id AS gameId, player_x AS playerX, player_o AS playerO, board_state AS boardState, turn, status FROM marubatsu_game WHERE game_id = #{gameId}")
  MarubatsuGame findByGameId(String gameId);

  @Update("UPDATE marubatsu_game SET board_state = #{boardState}, turn = #{turn}, status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE game_id = #{gameId}")
  void update(String gameId, String boardState, String turn, String status);

  @Select("SELECT game_id AS gameId, player_x AS playerX, player_o AS playerO, board_state AS boardState, turn, status FROM marubatsu_game WHERE player_x = #{player} OR player_o = #{player} LIMIT 1")
  MarubatsuGame findByPlayer(String player);

  @Select("SELECT game_id AS gameId, player_x AS playerX, player_o AS playerO, board_state AS boardState, turn, status FROM marubatsu_game WHERE status = 'playing' ORDER BY updated_at DESC")
  List<MarubatsuGame> findPlayingGames();

  @Delete("DELETE FROM marubatsu_game WHERE game_id = #{gameId}")
  void deleteByGameId(String gameId);
}
