package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GomokuMoveMapper {
  @Insert("INSERT INTO gomoku_move (game_id, player, x, y, move_no, created_at) VALUES (#{gameId}, #{player}, #{x}, #{y}, #{moveNo}, CURRENT_TIMESTAMP)")
  void insert(String gameId, String player, int x, int y, int moveNo);

  @Select("SELECT COUNT(*) FROM gomoku_move WHERE game_id = #{gameId}")
  int countByGameId(String gameId);

  @Select("SELECT id, game_id AS gameId, player, x, y, move_no AS moveNo, created_at AS createdAt FROM gomoku_move WHERE game_id = #{gameId} ORDER BY move_no")
  List<GomokuMove> findByGameId(String gameId);
}
