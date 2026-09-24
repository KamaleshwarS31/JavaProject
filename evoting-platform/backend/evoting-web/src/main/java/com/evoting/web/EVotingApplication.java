package com.evoting.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the E-Voting Platform.
 *
 * <p>Architecture: Privacy-Preserving, End-to-End Verifiable Blockchain-Based E-Voting System</p>
 * <p>Status: Academic Research Prototype — Not certified for production public elections.</p>
 *
 * <p>Component scan covers all com.evoting sub-packages across all modules.</p>
 */
@SpringBootApplication(scanBasePackages = "com.evoting")
@EntityScan(basePackages = "com.evoting")
@EnableJpaRepositories(basePackages = "com.evoting")
@EnableScheduling
public class EVotingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EVotingApplication.class, args);
    }
}
