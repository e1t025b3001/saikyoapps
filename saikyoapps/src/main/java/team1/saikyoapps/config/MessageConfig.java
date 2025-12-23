package team1.saikyoapps.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
public class MessageConfig {

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    // 指定 classpath 下的 i18n/messages 為 basename
    messageSource.setBasename("classpath:i18n/messages");
    // UTF-8 編碼，避免中文亂碼
    messageSource.setDefaultEncoding("UTF-8");
    // 如果找不到訊息，不直接回傳 key（可以改為 true 以便除錯）
    messageSource.setUseCodeAsDefaultMessage(false);
    // 快取秒數（開發時候可以設小一點）
    messageSource.setCacheSeconds(10);
    return messageSource;
  }
}
