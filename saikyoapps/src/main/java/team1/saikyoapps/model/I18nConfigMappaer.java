package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface I18nConfigMappaer {
  // 新規作成: login_user カラムを使う
  @Insert("INSERT INTO i18n_config (login_user, locale) VALUES (#{loginUser}, #{locale})")
  void insert(@Param("loginUser") String loginUser, @Param("locale") String locale);

  // 更新: login_user で検索して locale を更新
  @Update("UPDATE i18n_config SET locale = #{locale} WHERE login_user = #{loginUser}")
  void updateLocaleByUserName(@Param("loginUser") String loginUser, @Param("locale") String locale);

  // 参照: login_user を使ってレコードを取得
  @Select("SELECT id, login_user AS loginUser, locale FROM i18n_config WHERE login_user = #{loginUser}")
  I18nConfig findByUserName(@Param("loginUser") String loginUser);
}
