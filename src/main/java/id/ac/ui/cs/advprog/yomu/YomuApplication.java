package id.ac.ui.cs.advprog.yomu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = OAuth2ClientAutoConfiguration.class)
@EnableScheduling
public class YomuApplication {

    public static void main(String[] args) {
        SpringApplication.run(YomuApplication.class, args);
    }

}
