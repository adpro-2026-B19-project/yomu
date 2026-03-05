package id.ac.ui.cs.advprog.yomu.config;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("docker")
public class H2TcpServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer(@Value("${app.h2.tcp.port:9092}") String tcpPort) throws SQLException {
        return Server.createTcpServer(
                "-tcp",
                "-tcpAllowOthers",
                "-tcpPort",
                tcpPort
        );
    }
}
