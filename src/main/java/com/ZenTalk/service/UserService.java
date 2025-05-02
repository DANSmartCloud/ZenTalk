package com.ZenTalk.service;

import com.ZenTalk.model.User;
import com.ZenTalk.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UserService {
    private final UserRepository userRepository;
    private final JedisPool jedisPool;
    private final Key jwtKey;
    private static final long TOKEN_EXPIRE_HOURS = 24;
    private static final int VERIFICATION_CODE_EXPIRE_MINUTES = 5;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_ATTEMPTS_RESET_MINUTES = 30;

    public UserService(UserRepository userRepository, JedisPool jedisPool) {
        this.userRepository = userRepository;
        this.jedisPool = jedisPool;
        this.jwtKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    public User createUser(String username, String password, String email, String phone) {
        // 验证用户名、邮箱、手机号是否已存在
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("邮箱已被注册");
        }
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        // 使用Builder模式创建用户，密码加盐哈希
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User.Builder()
            .withUsername(username)
            .withPassword(hashedPassword)
            .withEmail(email)
            .withPhone(phone)
            .build();

        return userRepository.save(user);
    }

    public String login(String account, String password) {
        // 检查登录尝试次数
        String attemptsKey = "login_attempts:" + account;
        try (Jedis jedis = jedisPool.getResource()) {
            String attempts = jedis.get(attemptsKey);
            if (attempts != null && Integer.parseInt(attempts) >= MAX_LOGIN_ATTEMPTS) {
                throw new IllegalStateException("登录尝试次数过多，请稍后再试");
            }
        }

        // 查找用户
        Optional<User> userOpt = userRepository.findByUsername(account);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(account);
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(account);
        }

        if (userOpt.isEmpty() || !BCrypt.checkpw(password, userOpt.get().getPassword())) {
            // 增加失败计数
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.incr(attemptsKey);
                jedis.expire(attemptsKey, TimeUnit.MINUTES.toSeconds(LOGIN_ATTEMPTS_RESET_MINUTES));
            }
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 登录成功，重置失败计数
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(attemptsKey);
        }

        // 生成JWT token
        return generateToken(userOpt.get());
    }

    public boolean verifyToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void sendVerificationCode(String account) {
        // 生成6位验证码
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        String codeKey = "verification:" + account;
        
        try (Jedis jedis = jedisPool.getResource()) {
            // 设置验证码，5分钟过期
            jedis.setex(codeKey, TimeUnit.MINUTES.toSeconds(VERIFICATION_CODE_EXPIRE_MINUTES), code);
        }
        
        // TODO: 实际发送验证码到邮箱或手机
        // 这里应该调用邮件服务或短信服务
    }

    public boolean verifyCode(String account, String code) {
        String codeKey = "verification:" + account;
        try (Jedis jedis = jedisPool.getResource()) {
            String storedCode = jedis.get(codeKey);
            if (storedCode != null && storedCode.equals(code)) {
                jedis.del(codeKey); // 验证成功后删除
                return true;
            }
        }
        return false;
    }

    public void resetPassword(String account, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(account);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(account);
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(account);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
            user.setPassword(hashedPassword);
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private String generateToken(User user) {
        return Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setSubject(user.getUsername())
            .claim("userId", user.getId())
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(TimeUnit.HOURS.toSeconds(TOKEN_EXPIRE_HOURS))))
            .signWith(jwtKey)
            .compact();
    }
}