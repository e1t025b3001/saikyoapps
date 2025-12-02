package team1.saikyoapps.model;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DarourPlayerMapper {

  @Insert("INSERT INTO darour_players (player_id, darour_id, user_id, name, hand_json, position) VALUES (#{playerId}, #{darourGameId}, #{userId}, #{name}, #{handJson}, #{position})")
  void insertDarourPlayer(DarourPlayer p);

  @Select("SELECT player_id AS playerId, darour_id AS darourGameId, user_id AS userId, name, hand_json AS handJson, position FROM darour_players WHERE darour_id = #{darourGameId}")
  List<DarourPlayer> selectDarourPlayersByDarourGameId(@Param("darourGameId") String darourGameId);
}
