package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface I18nConfigMappaer {
  // 新規作成: 指定ユーザのロケールを挿入
  @Insert("INSERT INTO i18n_config (user, locale) VALUES (#{user}, #{locale})")
  void insert(String user, String locale);

  // 更新: ユーザーネームでロケールを更新
  @Update("UPDATE i18n_config SET locale = #{locale} WHERE user = #{user}")
  void updateLocaleByUserName(String user, String locale);

  // 参照: ユーザーネームでレコードを取得
  @Select("SELECT user, locale FROM i18n_config WHERE user = #{user}")
  I18nConfig findByUserName(String user);
}
