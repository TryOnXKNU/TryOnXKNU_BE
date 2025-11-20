package org.example.tryonx.apple.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AppleAuthService {

    @Value("${apple.client.id}")
    private String clientId;

    @Value("${apple.team.id}")
    private String teamId;

    @Value("${apple.key.id}")
    private String keyId;

    @Value("${apple.key.path}")
    private String keyPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${apple.auth.url:https://appleid.apple.com}")
    private String appleAuthUrl;

    @Value("${apple.redirect.url:}")
    private String redirectUrl;

    public String generateClientSecret() throws Exception {
        // Load private key from classpath resource (handle paths like src/main/resources/...)
        String filename = Paths.get(keyPath).getFileName().toString();
        Resource resource = new ClassPathResource(filename);
        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            String pem = new String(bytes, StandardCharsets.UTF_8);
            String base64 = pem.replaceAll("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey privateKey = kf.generatePrivate(keySpec);

            // Build JWT
            Instant now = Instant.now();
            Date iat = Date.from(now);
            Date exp = Date.from(now.plusSeconds(60L * 60L * 24L * 180L)); // 180 days

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(teamId)
                    .subject(clientId)
                    .audience("https://appleid.apple.com")
                    .issueTime(iat)
                    .expirationTime(exp)
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(keyId)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            JWSSigner signer = new ECDSASigner((ECPrivateKey) privateKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        }
    }

    public Map<String, Object> exchangeCode(String code) throws Exception {
        String clientSecret = generateClientSecret();

        String form = "grant_type=authorization_code&code=" + java.net.URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://appleid.apple.com/auth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400) {
            throw new IllegalStateException("Apple token endpoint returned " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode node = objectMapper.readTree(resp.body());
        Map<String, Object> result = new HashMap<>();
        if (node.has("id_token")) result.put("id_token", node.get("id_token").asText());
        if (node.has("access_token")) result.put("access_token", node.get("access_token").asText());
        if (node.has("refresh_token")) result.put("refresh_token", node.get("refresh_token").asText());

        // Optionally verify id_token now
        if (result.containsKey("id_token")) {
            Map<String, Object> claims = verifyIdToken(result.get("id_token").toString());
            result.putAll(claims);
        }

        return result;
    }

    public Map<String, Object> verifyIdToken(String idToken) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(idToken);

        // Fetch Apple's JWK set
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://appleid.apple.com/auth/keys")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        JWKSet jwkSet = JWKSet.parse(resp.body());

        String kid = signedJWT.getHeader().getKeyID();
        JWK jwk = jwkSet.getKeyByKeyId(kid);
        if (jwk == null) throw new IllegalStateException("Unable to find JWK for kid: " + kid);

        boolean verified = false;
        if (jwk instanceof RSAKey) {
            RSAKey rsa = (RSAKey) jwk;
            verified = signedJWT.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(rsa.toRSAPublicKey()));
        } else {
            // Apple keys are RSA; if not, try general verification
            verified = signedJWT.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(((RSAKey) jwk).toRSAPublicKey()));
        }

        if (!verified) throw new IllegalStateException("id_token signature verification failed");

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // Validate standard claims according to Apple's docs
        String issuer = claims.getIssuer();
        if (!"https://appleid.apple.com".equals(issuer)) {
            throw new IllegalStateException("Invalid id_token issuer: " + issuer);
        }

        if (claims.getAudience() == null || !claims.getAudience().contains(clientId)) {
            throw new IllegalStateException("id_token audience does not contain client_id");
        }

        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
            throw new IllegalStateException("id_token is expired");
        }

        Map<String, Object> out = new HashMap<>();
        out.put("sub", claims.getSubject());
        try {
            out.put("email", claims.getStringClaim("email"));
        } catch (Exception ex) {
            // email may not always be present
        }
        out.put("iss", issuer);
        out.put("aud", claims.getAudience());
        out.put("exp", exp);
        // include email_verified if present
        try {
            out.put("email_verified", claims.getStringClaim("email_verified"));
        } catch (Exception ignored) {}

        return out;
    }

    public String getAuthorizationUrl() {
        // Build URL: {appleAuthUrl}/auth/authorize?client_id=...&redirect_uri=...&response_type=code%20id_token&scope=name%20email&response_mode=form_post
        String encodedRedirect = java.net.URLEncoder.encode(redirectUrl == null ? "" : redirectUrl, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        sb.append(appleAuthUrl.replaceAll("/$", ""));
        sb.append("/auth/authorize");
        sb.append("?client_id=").append(java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        sb.append("&redirect_uri=").append(encodedRedirect);
        sb.append("&response_type=code%20id_token");
        sb.append("&scope=name%20email");
        sb.append("&response_mode=form_post");
        return sb.toString();
    }
}
