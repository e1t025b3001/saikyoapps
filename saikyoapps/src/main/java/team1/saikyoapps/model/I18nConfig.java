package team1.saikyoapps.model;

public class I18nConfig {
  // Mapper の alias に合わせて loginUser を使用
  private String loginUser;
  private String locale;

  public String getLoginUser() {
    return loginUser;
  }

  public void setLoginUser(String loginUser) {
    this.loginUser = loginUser;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }
}
