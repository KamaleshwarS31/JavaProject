package com.evoting.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
        loadDotEnv();
        SpringApplication.run(EVotingApplication.class, args);
    }

    /**
     * Automatically loads .env file from project root or working directory
     * into System properties if not already provided via environment variables.
     * Ensures smooth execution across IDEs, CLI, and test runners without manual exports.
     */
    private static void loadDotEnv() {
        Path[] searchPaths = {
            Paths.get(".env"),
            Paths.get("../.env"),
            Paths.get("../../.env"),
            Paths.get("../../../.env"),
            Paths.get(System.getProperty("user.dir"), ".env"),
            Paths.get(System.getProperty("user.dir"), "..", ".env"),
            Paths.get(System.getProperty("user.dir"), "..", "..", ".env")
        };

        for (Path path : searchPaths) {
            if (Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (String line : lines) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            int eqIdx = line.indexOf('=');
                            if (eqIdx > 0) {
                                String key = line.substring(0, eqIdx).trim();
                                String value = line.substring(eqIdx + 1).trim();
                                // Only set if not already present in environment or system properties
                                if (System.getProperty(key) == null && System.getenv(key) == null) {
                                    System.setProperty(key, value);
                                }
                            }
                        }
                    }
                    System.out.println("[E-Voting Platform] Configuration loaded from .env at: " + path.toAbsolutePath().normalize());
                    return;
                } catch (Exception e) {
                    System.err.println("[E-Voting Platform] Notice: Could not read .env from " + path + ": " + e.getMessage());
                }
            }
        }
    }
}
