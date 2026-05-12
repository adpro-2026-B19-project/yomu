package id.ac.ui.cs.advprog.yomu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YomuApplication {

    public static void main(String[] args) {
        SpringApplication.run(YomuApplication.class, args);
    }

}
