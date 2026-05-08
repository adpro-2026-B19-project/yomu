package id.ac.ui.cs.advprog.yomu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;

@SpringBootApplication(exclude = OAuth2ClientAutoConfiguration.class)
public class YomuApplication {

    static void main(String[] args) {
        SpringApplication.run(YomuApplication.class, args);
    }

}
