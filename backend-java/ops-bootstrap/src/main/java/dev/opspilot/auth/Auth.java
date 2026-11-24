package dev.opspilot.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.opspilot.api.ApiFailure;
import dev.opspilot.store.OpsStore;
import dev.opspilot.store.Rows.Account;
import jakarta.servlet.DispatcherType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** 提供登录及租户账号管理，JWT 中的角色和租户由服务端签发。 */
@RestController
@RequestMapping("/api/v1/auth")
public class Auth {
    public static final String IDENTIFIER = "[a-z0-9][a-z0-9_-]{0,47}";
    private static final String ISSUER = "opspilot";
    private final OpsStore store;
    private final BCryptPasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final TransactionTemplate transaction;
    private final String dummyHash;

    /** 注入密码编码器、签发器及账号写入事务。 */
    public Auth(OpsStore store, BCryptPasswordEncoder passwords, JwtEncoder encoder,
                TransactionTemplate transaction) {
        this.store = store;
        this.passwords = passwords;
        this.encoder = encoder;
        this.transaction = transaction;
        this.dummyHash = passwords.encode("invalid-account-placeholder");
    }

    /** 允许的租户角色。 */
    public enum Role { ADMIN, OPERATOR, VIEWER }

    /** 登录负载。 */
    public record Login(@Pattern(regexp = IDENTIFIER) @NotBlank String tenant,
                        @Pattern(regexp = IDENTIFIER) @NotBlank String username,
                        @NotBlank @Size(max = 72) String password) {}

    /** 管理员创建同租户账号的负载。 */
    public record NewAccount(@Pattern(regexp = IDENTIFIER) @NotBlank String username,
                             @NotBlank @Size(min = 12, max = 72) String password,
                             @jakarta.validation.constraints.NotNull Role role) {}

    /** 短时 Bearer 凭证。 */
    public record Token(String accessToken, String tokenType, long expiresIn) {}

    /** 检查登录凭据，签发有效期 30 分钟的 HS256 JWT。 */
    @PostMapping("/token")
    public Token login(@Valid @RequestBody Login request) {
        Account account = store.account(request.tenant(), request.username());
        String hash = account == null ? dummyHash : account.passwordHash();
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(request.password(), hash) || account == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject(account.username())
                .issuedAt(now).expiresAt(now.plusSeconds(1800))
                .claim("tenant", account.tenantId()).claim("role", account.role()).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Token(token, "Bearer", 1800);
    }

    /** 在管理员所属租户创建账号，并与审计记录一同提交。 */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt actor,
                       @Valid @RequestBody NewAccount request) {
        checkPasswordLength(request.password());
        String encoded = passwords.encode(request.password());
        String tenant = actor.getClaimAsString("tenant");
        try {
            transaction.executeWithoutResult(status -> {
                store.insertAccount(tenant, request.username(), encoded, request.role().name());
                store.audit(tenant, "ACCOUNT", request.username(), actor.getSubject(), "ACCOUNT_CREATED",
                        request.role().name());
            });
        } catch (DuplicateKeyException ex) {
            throw ApiFailure.conflict("Account already exists");
        }
    }

    /** BCrypt 按 UTF-8 字节计数，避免提交会被截断的密码。 */
    private static void checkPasswordLength(String password) {
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password length out of bounds");
        }
    }

    /** 初始化签发、验证、角色映射与 Web 权限。 */
    @Configuration(proxyBeanMethods = false)
    static class SecuritySetup {
        /** 创建 BCrypt 编码器。 */
        @Bean
        BCryptPasswordEncoder passwords() {
            return new BCryptPasswordEncoder();
        }

        /** 从环境加载至少 32 字节的签名密钥。 */
        @Bean
        SecretKey jwtSecret(@Value("${opspilot.auth.jwt-key}") String value) {
            byte[] key = value.getBytes(StandardCharsets.UTF_8);
            if (key.length < 32) {
                throw new IllegalStateException("OPSPILOT_JWT_KEY requires at least 32 UTF-8 bytes");
            }
            return new SecretKeySpec(key, "HmacSHA256");
        }

        /** 配置 HS256 JWT 签发器。 */
        @Bean
        JwtEncoder jwtEncoder(SecretKey secret) {
            return new NimbusJwtEncoder(new ImmutableSecret<>(secret));
        }

        /** 验证算法、发行方和凭据时效。 */
        @Bean
        JwtDecoder jwtDecoder(SecretKey secret) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secret).macAlgorithm(MacAlgorithm.HS256).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
            return decoder;
        }

        /** 对 Bearer API 实施角色限制，默认拒绝未注册路由。 */
        @Bean
        SecurityFilterChain security(HttpSecurity http) throws Exception {
            JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
            roles.setAuthoritiesClaimName("role");
            roles.setAuthorityPrefix("ROLE_");
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(roles);
            return http.csrf(csrf -> csrf.disable())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(rules -> rules
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/token").permitAll()
                        .requestMatchers("/actuator/prometheus", "/api/v1/auth/users").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/**")
                        .hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/**")
                        .hasAnyRole("ADMIN", "OPERATOR")
                        .anyRequest().denyAll())
                    .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                    .build();
        }

        /** 使用启动配置初始化一个租户管理员，重复执行保留现有账号。 */
        @Bean
        ApplicationRunner bootstrap(OpsStore store, BCryptPasswordEncoder passwords, TransactionTemplate tx,
                                    @Value("${opspilot.auth.bootstrap-tenant}") String tenant,
                                    @Value("${opspilot.auth.bootstrap-user}") String user,
                                    @Value("${opspilot.auth.bootstrap-password}") String password) {
            return args -> {
                if (tenant.isEmpty() && user.isEmpty() && password.isEmpty()) {
                    return;
                }
                if (!tenant.matches(IDENTIFIER) || !user.matches(IDENTIFIER)) {
                    throw new IllegalArgumentException("Invalid bootstrap tenant or username");
                }
                checkPasswordLength(password);
                String hash = passwords.encode(password);
                tx.executeWithoutResult(status -> {
                    store.ensureTenant(tenant);
                    store.seedAccount(tenant, user, hash);
                });
            };
        }
    }
}
