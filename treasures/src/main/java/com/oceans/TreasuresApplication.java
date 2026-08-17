package com.oceans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The application entry point.
 *
 * @SpringBootApplication does three things at once: enables auto-configuration
 * (Spring wires up Tomcat, JSON handling, etc. based on what's on the classpath),
 * marks this as a configuration class, and turns on component scanning of this
 * package (com.oceans) and everything below it — which is how it will discover the
 * GameController you add next.
 *
 * Goes in: src/main/java/com/oceans/TreasuresApplication.java
 */
@SpringBootApplication
public class TreasuresApplication {
    public static void main(String[] args) {
        SpringApplication.run(TreasuresApplication.class, args);
    }
}
