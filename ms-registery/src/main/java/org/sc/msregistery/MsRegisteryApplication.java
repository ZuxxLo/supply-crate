package org.sc.msregistery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer

public class MsRegisteryApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsRegisteryApplication.class, args);
    }

}
