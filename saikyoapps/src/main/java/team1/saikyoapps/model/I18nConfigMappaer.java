package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface I18nConfigMappaer {
  // 新規作成: 指定ユーザのロケールを挿入
  @Insert("INSERT INTO i18n_config (user_name, locale) VALUES (#{userName}, #{locale})")
  void insert(String userName, String locale);

  // 更新: ユーザーネームでロケールを更新
  @Update("UPDATE i18n_config SET locale = #{locale} WHERE user_name = #{userName}")
  void updateLocaleByUserName(String userName, String locale);

  // 参照: ユーザーネームでレコードを取得
  @Select("SELECT user_name AS user, locale FROM i18n_config WHERE user_name = #{userName}")
  I18nConfig findByUserName(String userName);
}
