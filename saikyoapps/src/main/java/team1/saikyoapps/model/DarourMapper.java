package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DarourMapper {

  // DarourGame 用の情報を挿入する
  @Insert("INSERT INTO darour_games (darour_id, owner_user_id, state_json, created_at, updated_at) VALUES (#{darourGameId}, #{ownerUserId}, #{stateJson}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
  void insertDarour(Darour darour);

  // DarourGame を ID で取得する
  @Select("SELECT darour_id AS darourGameId, owner_user_id AS ownerUserId, state_json AS stateJson, created_at AS createdAt, updated_at AS updatedAt FROM darour_games WHERE darour_id = #{darourGameId}")
  Darour selectDarourById(@Param("darourGameId") String darourGameId);

  // DarourGame の状態を更新する
  @Update("UPDATE darour_games SET state_json = #{stateJson}, updated_at = CURRENT_TIMESTAMP WHERE darour_id = #{darourGameId}")
  void updateDarourState(@Param("darourGameId") String darourGameId, @Param("stateJson") String stateJson);

  // DarourGame を削除する
  @Delete("DELETE FROM darour_games WHERE darour_id = #{darourGameId}")
  void deleteDarour(@Param("darourGameId") String darourGameId);
}
