package id.ac.ui.cs.advprog.yomu.config;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("docker")
public class H2TcpServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = "app.h2.tcp.enabled", havingValue = "true")
    public Server h2TcpServer(
            @Value("${app.h2.tcp.port:9092}") String tcpPort,
            @Value("${app.h2.tcp.allow-others:false}") boolean tcpAllowOthers
    ) throws SQLException {
        if (tcpAllowOthers) {
            return Server.createTcpServer(
                    "-tcp",
                    "-tcpAllowOthers",
                    "-tcpPort",
                    tcpPort
            );
        }
        return Server.createTcpServer(
                "-tcp",
                "-tcpPort",
                tcpPort
        );
    }
}
