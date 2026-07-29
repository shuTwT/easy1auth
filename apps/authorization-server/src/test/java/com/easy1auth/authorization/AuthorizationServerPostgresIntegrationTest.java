package com.easy1auth.authorization;

import com.easy1auth.application.ApplicationService;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.directory.PoolUserService;
import com.easy1auth.tenant.TenantService;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.URI;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(properties={"spring.flyway.enabled=true","easy1auth.oauth2.key-encryption-secret=abcdefghijklmnopqrstuvwxyz012345","easy1auth.oauth2.issuer-base=http://localhost","easy1auth.security.data-encryption-secret=abcdefghijklmnopqrstuvwxyz012345"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker=true)
class AuthorizationServerPostgresIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry){registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);registry.add("spring.datasource.username",POSTGRES::getUsername);registry.add("spring.datasource.password",POSTGRES::getPassword);}
    @Autowired JdbcClient db;@Autowired TenantService tenants;@Autowired ApplicationService applications;@Autowired PoolUserService users;@Autowired MockMvc mvc;

    @Test void clientCredentialsDiscoveryJwkAndEncryptedPersistentKeyWorkTogether()throws Exception{
        UUID account=account();var tenant=tenants.create(account,"OAuth tenant","basic");
        var app=applications.create(tenant.id(),new ApplicationService.ApplicationInput("Machine",null,null,"machine",List.of(),List.of(),List.of("client_credentials"),List.of("api.read"),false,false,300,3600));
        assertThat(app.clientSecret()).isNotBlank();
        String stored=db.sql("select client_secret_hash from oauth_application where id=:id").param("id",app.id()).query(String.class).single();
        assertThat(stored).isNotEqualTo(app.clientSecret()).startsWith("$2");

        mvc.perform(get("/t/{tenant}/.well-known/openid-configuration",tenant.id())).andExpect(status().isOk()).andExpect(jsonPath("$.issuer").value("http://localhost/t/"+tenant.id())).andExpect(jsonPath("$.token_endpoint").value("http://localhost/t/"+tenant.id()+"/oauth2/token"));
        mvc.perform(get("/t/{tenant}/oauth2/jwks",tenant.id())).andExpect(status().isOk()).andExpect(jsonPath("$.keys[0].kty").value("RSA"));

        String basic=Base64.getEncoder().encodeToString((app.clientId()+":"+app.clientSecret()).getBytes(StandardCharsets.UTF_8));
        String json=mvc.perform(post("/t/{tenant}/oauth2/token",tenant.id()).header("Authorization","Basic "+basic).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type","client_credentials").param("scope","api.read"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.access_token").isString()).andExpect(jsonPath("$.token_type").value("Bearer")).andReturn().getResponse().getContentAsString();
        String token=new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("access_token").asText();var jwt=SignedJWT.parse(token);
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("http://localhost/t/"+tenant.id());assertThat(jwt.getJWTClaimsSet().getAudience()).contains(app.clientId());assertThat(jwt.getJWTClaimsSet().getStringClaim("tenant_id")).isEqualTo(tenant.id().toString());assertThat(jwt.getJWTClaimsSet().getStringClaim("subject_type")).isEqualTo("oauth_client");
        String encrypted=db.sql("select encrypted_private_jwk from oauth2_signing_key where tenant_id=:tenant").param("tenant",tenant.id()).query(String.class).single();assertThat(encrypted).doesNotContain("\"d\"").doesNotContain("PRIVATE");
    }
    @Test void authorizationCodePkceRefreshUserInfoAndRevocationFlowIsTenantBound()throws Exception{
        UUID account=account();var tenant=tenants.create(account,"OIDC tenant","basic");String redirect="https://client.example/callback";
        var app=applications.create(tenant.id(),new ApplicationService.ApplicationInput("SPA",null,null,"spa",List.of(redirect),List.of("https://client.example/logout"),List.of("authorization_code","refresh_token"),List.of("openid","profile","email"),true,false,300,3600));
        var poolUser=users.create(tenant.id(),new PoolUserService.Input("alice","alice@oidc.test","Password1",null,"Alice",null,null,null,null,null));
        String verifier="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~abc";String challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        var authenticated=user(poolUser.id().toString()).authorities(new SimpleGrantedAuthority("ROLE_POOL_USER"),new SimpleGrantedAuthority("TENANT_"+tenant.id()));
        mvc.perform(get("/t/{tenant}/oauth2/authorize",tenant.id()).with(authenticated).param("response_type","code").param("client_id",app.clientId()).param("redirect_uri","https://evil.example/callback").param("scope","openid")).andExpect(status().isBadRequest());
        mvc.perform(get("/t/{tenant}/oauth2/authorize",tenant.id()).with(authenticated).param("response_type","code").param("client_id",app.clientId()).param("redirect_uri",redirect).param("scope","openid")).andExpect(status().isBadRequest());
        String location=mvc.perform(get("/t/{tenant}/oauth2/authorize",tenant.id()).with(authenticated)
                        .param("response_type","code").param("client_id",app.clientId()).param("redirect_uri",redirect).param("scope","openid profile email").param("state","state-1").param("nonce","nonce-1").param("code_challenge",challenge).param("code_challenge_method","S256"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith(redirect);var query=org.springframework.web.util.UriComponentsBuilder.fromUri(URI.create(location)).build().getQueryParams();String code=query.getFirst("code");assertThat(code).isNotBlank();assertThat(query.getFirst("state")).isEqualTo("state-1");

        String tokenJson=mvc.perform(post("/t/{tenant}/oauth2/token",tenant.id()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type","authorization_code").param("client_id",app.clientId()).param("code",code).param("redirect_uri",redirect).param("code_verifier",verifier))
                .andExpect(status().isOk()).andExpect(jsonPath("$.access_token").isString()).andExpect(jsonPath("$.refresh_token").isString()).andExpect(jsonPath("$.id_token").isString()).andReturn().getResponse().getContentAsString();
        var tokens=new com.fasterxml.jackson.databind.ObjectMapper().readTree(tokenJson);String access=tokens.get("access_token").asText(),refresh=tokens.get("refresh_token").asText();
        mvc.perform(post("/t/{tenant}/oauth2/token",tenant.id()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type","authorization_code").param("client_id",app.clientId()).param("code",code).param("redirect_uri",redirect).param("code_verifier",verifier)).andExpect(status().isBadRequest());
        mvc.perform(get("/t/{tenant}/userinfo",tenant.id()).header("Authorization","Bearer "+access)).andExpect(status().isOk()).andExpect(jsonPath("$.sub").value(poolUser.id().toString())).andExpect(jsonPath("$.email").value("alice@oidc.test")).andExpect(jsonPath("$.tenant_id").value(tenant.id().toString()));

        String refreshedJson=mvc.perform(post("/t/{tenant}/oauth2/token",tenant.id()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type","refresh_token").param("client_id",app.clientId()).param("refresh_token",refresh)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String rotated=new com.fasterxml.jackson.databind.ObjectMapper().readTree(refreshedJson).get("refresh_token").asText();assertThat(rotated).isNotEqualTo(refresh);
        mvc.perform(post("/t/{tenant}/oauth2/revoke",tenant.id()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("client_id",app.clientId()).param("token",rotated).param("token_type_hint","refresh_token")).andExpect(status().isOk());
        mvc.perform(post("/t/{tenant}/oauth2/token",tenant.id()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type","refresh_token").param("client_id",app.clientId()).param("refresh_token",rotated)).andExpect(status().isBadRequest());

        UUID otherAccount=account();var other=tenants.create(otherAccount,"Other tenant","basic");
        mvc.perform(post("/t/{tenant}/oauth2/token",other.id()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type","refresh_token").param("client_id",app.clientId()).param("refresh_token",refresh)).andExpect(status().isUnauthorized());
    }
    private UUID account(){UUID id=UuidV7.randomUuid();db.sql("insert into admin_account(id,username,email,status) values(:id,:u,:e,'active')").param("id",id).param("u","oauth-"+id).param("e",id+"@oauth.test").update();return id;}
}
