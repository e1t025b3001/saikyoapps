package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MarubatsuMoveMapper {
  @Insert("INSERT INTO marubatsu_move (game_id, player, x, y, move_no, created_at) VALUES (#{gameId}, #{player}, #{x}, #{y}, #{moveNo}, CURRENT_TIMESTAMP)")
  void insert(String gameId, String player, int x, int y, int moveNo);

  @Select("SELECT COUNT(*) FROM marubatsu_move WHERE game_id = #{gameId}")
  int countByGameId(String gameId);

  @Select("SELECT id, game_id AS gameId, player, x, y, move_no AS moveNo, created_at AS createdAt FROM marubatsu_move WHERE game_id = #{gameId} ORDER BY move_no")
  List<MarubatsuMove> findByGameId(String gameId);

  @Delete("DELETE FROM marubatsu_move WHERE game_id = #{gameId}")
  void deleteByGameId(String gameId);

  // 新增：計算特定玩家在該局的棋子數
  @Select("SELECT COUNT(*) FROM marubatsu_move WHERE game_id = #{gameId} AND player = #{player}")
  int countByGameIdAndPlayer(String gameId, String player);

  // 新增：找出該玩家在該局最早的一筆落子（依 created_at 或 move_no 升序）
  @Select("SELECT id, game_id AS gameId, player, x, y, move_no AS moveNo, created_at AS createdAt FROM marubatsu_move WHERE game_id = #{gameId} AND player = #{player} ORDER BY created_at ASC LIMIT 1")
  MarubatsuMove findEarliestByGameIdAndPlayer(String gameId, String player);

  // 新增：依 id 刪除單筆落子
  @Delete("DELETE FROM marubatsu_move WHERE id = #{id}")
  void deleteById(long id);
}
