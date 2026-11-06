package com.letssign.api.config;

import com.letssign.api.domain.User;
import com.letssign.api.domain.UserRole;
import com.letssign.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Semeia o Admin padrão (mesmas credenciais do app: admin@letssign.com / admin123). */
@Configuration
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    ApplicationRunner seedAdmin(UserRepository users, PasswordEncoder encoder, LetsSignProperties props) {
        return args -> {
            if (users.findByEmailIgnoreCase(props.adminEmail()).isPresent()) return;
            User admin = new User();
            admin.fullName = "Administrador";
            admin.email = props.adminEmail();
            admin.phone = "00000000";
            admin.passwordHash = encoder.encode(props.adminPassword());
            admin.role = UserRole.ADMIN;
            users.save(admin);
            log.info("Admin semeado: {}", props.adminEmail());
        };
    }
}
