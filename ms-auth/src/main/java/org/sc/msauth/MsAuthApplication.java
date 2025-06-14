package org.sc.msauth;

import org.sc.msauth.entities.Role;
import org.sc.msauth.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.sql.Date;
import java.util.List;

@SpringBootApplication
@Import(org.sc.commonconfig.JwtUtil.class)

 public class MsAuthApplication implements CommandLineRunner {

     @Autowired
     RoleRepository roleRepository;

    public static void main(String[] args) {
        SpringApplication.run(MsAuthApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        List<String> roles = List.of("USER", "ADMIN");

        for (String roleName : roles) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
        }
    }
}
