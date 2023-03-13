package com.gitbitex.marketdata.manager;

import com.gitbitex.marketdata.entity.User;
import com.gitbitex.marketdata.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserManager {
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;
    private final AccountManager accountManager;

    public User createUser(String email, String password) {
        // check if the email address is already registered
        User user = userRepository.findByEmail(email);
        if (user != null) {
            throw new RuntimeException("duplicate email address");
        }

        // create new user
        user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordSalt(UUID.randomUUID().toString());
        user.setPasswordHash(encryptPassword(password, user.getPasswordSalt()));
        userRepository.save(user);
        return user;
    }

    public String generateAccessToken(User user, String sessionId) {
        String accessToken = user.getId() + ":" + sessionId + ":" + generateAccessTokenSecret(user);
        redissonClient.getBucket(redisKeyForAccessToken(accessToken))
                .set(new Date().toString(), 14, TimeUnit.DAYS);
        return accessToken;
    }

    public void deleteAccessToken(String accessToken) {
        redissonClient.getBucket(redisKeyForAccessToken(accessToken)).delete();
    }

    public User getUserByAccessToken(String accessToken) {
        if (accessToken == null) {
            return null;
        }

        Object val = redissonClient.getBucket(redisKeyForAccessToken(accessToken)).get();
        if (val == null) {
            return null;
        }

        String[] parts = accessToken.split(":");
        if (parts.length != 3) {
            return null;
        }

        String userId = parts[0];
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            return null;
        }

        // check secret
        if (!parts[2].equals(generateAccessTokenSecret(user))) {
            return null;
        }
        return user;
    }

    public User getUser(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return null;
        }

        if (user.getPasswordHash().equals(encryptPassword(password, user.getPasswordSalt()))) {
            return user;
        }
        return null;
    }

    private String encryptPassword(String password, String saltKey) {
        return DigestUtils.md5DigestAsHex((password + saltKey).getBytes(StandardCharsets.UTF_8));
    }

    private String generateAccessTokenSecret(User user) {
        String key = user.getId() + user.getEmail() + user.getPasswordHash();
        return DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8));
    }

    private String redisKeyForAccessToken(String accessToken) {
        return "token." + accessToken;
    }
}
