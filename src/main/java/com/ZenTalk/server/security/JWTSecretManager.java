package com.ZenTalk.server.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JWTSecretManager {
    private static final long ROTATION_PERIOD_HOURS = 24;
    private static Key currentSecret;
    private Key previousSecret;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // Generate initial keys
        currentSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        previousSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512);

        // Schedule periodic rotation
        scheduler.scheduleAtFixedRate(
            this::rotateSecret,
            ROTATION_PERIOD_HOURS,
            ROTATION_PERIOD_HOURS,
            TimeUnit.HOURS
        );
    }

    private synchronized void rotateSecret() {
        previousSecret = currentSecret;
        currentSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    public Key getCurrentSecret() {
        return currentSecret;
    }

    public Key getPreviousSecret() {
        return previousSecret;
    }
    
    public static Key getSecretKey() {
        return currentSecret;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
