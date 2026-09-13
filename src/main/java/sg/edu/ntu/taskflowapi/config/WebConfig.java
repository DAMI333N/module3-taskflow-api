package sg.edu.ntu.taskflowapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Lets the task-manager frontend (Vite dev server) call the API from the browser
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final String[] allowedOrigins;

  public WebConfig(@Value("${taskflow.cors.allowed-origins}") String[] allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
  }
}
