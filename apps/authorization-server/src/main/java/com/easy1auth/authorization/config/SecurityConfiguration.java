package com.easy1auth.authorization.config;

import com.easy1auth.directory.*;
import com.easy1auth.oauth2.*;
import com.easy1auth.useraccess.UserAccessCatalogService;
import com.easy1auth.security.SecurityPolicyService;
import com.easy1auth.authorization.security.IssuerHostValidationFilter;
import com.easy1auth.authorization.security.TenantPrincipalValidationFilter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.http.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}

    @Bean FilterRegistrationBean<IssuerHostValidationFilter> issuerHostValidationFilterRegistration(IssuerHostValidationFilter filter){
        var registration=new FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;
    }

    @Bean FilterRegistrationBean<TenantPrincipalValidationFilter> tenantPrincipalValidationFilterRegistration(TenantPrincipalValidationFilter filter){
        var registration=new FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;
    }

    @Bean AuthenticationProvider poolUserAuthenticationProvider(PoolUserAuthenticationService users,SecurityPolicyService security,com.easy1auth.security.PoolUserDeviceService devices){
        return new AuthenticationProvider(){
            @Override public Authentication authenticate(Authentication authentication)throws AuthenticationException{
                PoolLoginDetails details=authentication.getDetails()instanceof PoolLoginDetails value?value:null;String tenant=details==null?null:details.tenant();
                try{
                    var principal=users.authenticate(UUID.fromString(tenant),authentication.getName(),String.valueOf(authentication.getCredentials()));
                    if(principal==null)throw new BadCredentialsException("用户名或密码错误");devices.seen(principal.tenantId(),principal.id(),details.userAgent(),details.ip());
                    var authorities=new ArrayList<GrantedAuthority>();authorities.add(new SimpleGrantedAuthority("ROLE_POOL_USER"));authorities.add(new SimpleGrantedAuthority("TENANT_"+principal.tenantId()));var mfa=security.status("pool_user",principal.id());if(mfa.enabled()||security.policy(principal.tenantId()).mfaRequired())authorities.add(new SimpleGrantedAuthority("MFA_REQUIRED"));
                    return UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(principal.id().toString()).password("").authorities(authorities).build(),null,authorities);
                }catch(IllegalArgumentException ex){throw new BadCredentialsException("租户或凭据无效");}
            }
            @Override public boolean supports(Class<?> authentication){return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);}
        };
    }

    @Bean @Order(1)
    SecurityFilterChain authorizationServerSecurity(HttpSecurity http,RegisteredClientRepository clients,OAuth2AuthorizationService authorizations,OAuth2AuthorizationConsentService consents,IssuerHostValidationFilter issuerHostValidation,TenantPrincipalValidationFilter tenantPrincipalValidation) throws Exception {
        var configurer=OAuth2AuthorizationServerConfigurer.authorizationServer();
        http.securityMatcher(configurer.getEndpointsMatcher())
                .with(configurer,server->server.registeredClientRepository(clients).authorizationService(authorizations).authorizationConsentService(consents)
                        .authorizationEndpoint(endpoint->endpoint.consentPage("/oauth-consent")).oidc(Customizer.withDefaults()))
                .csrf(csrf->csrf.ignoringRequestMatchers(configurer.getEndpointsMatcher()))
                .addFilterAfter(issuerHostValidation,SecurityContextHolderFilter.class)
                .addFilterAfter(tenantPrincipalValidation,IssuerHostValidationFilter.class)
                .authorizeHttpRequests(auth->auth.anyRequest().authenticated())
                .oauth2ResourceServer(resource->resource.jwt(Customizer.withDefaults()))
                .exceptionHandling(errors->errors.defaultAuthenticationEntryPointFor((request,response,exception)->{
                    String[] parts=request.getRequestURI().split("/");String tenant=parts.length>2&&"t".equals(parts[1])?parts[2]:"";
                    response.sendRedirect("/oauth-login?tenant="+URLEncoder.encode(tenant,StandardCharsets.UTF_8));
                },new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain applicationSecurity(HttpSecurity http,AuthenticationProvider provider,SecurityPolicyService security)throws Exception{
        return http.authenticationProvider(provider)
                .authorizeHttpRequests(auth->auth.requestMatchers("/actuator/health","/actuator/health/**","/actuator/info","/livez","/readyz","/oauth-login","/oauth-login/mfa","/t/*/federation/**","/error").permitAll().anyRequest().authenticated())
                .formLogin(form->form.loginPage("/oauth-login").loginProcessingUrl("/oauth-login").authenticationDetailsSource(request->new PoolLoginDetails(request.getParameter("tenant"),request.getHeader("User-Agent"),request.getRemoteAddr())).successHandler((request,response,authentication)->{if(authentication.getAuthorities().stream().anyMatch(a->"MFA_REQUIRED".equals(a.getAuthority()))){UUID user=UUID.fromString(authentication.getName()),tenant=UUID.fromString(Objects.toString(request.getParameter("tenant")));var challenge=security.issueTotpChallenge("pool_user",user,tenant,"oidc_login");request.getSession(true).setAttribute("EASY1AUTH_MFA_CHALLENGE",challenge.token());request.getSession().setAttribute("EASY1AUTH_MFA_USER",user);request.getSession().setAttribute("EASY1AUTH_MFA_TENANT",tenant);org.springframework.security.core.context.SecurityContextHolder.clearContext();response.sendRedirect("/oauth-login/mfa");return;}new org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler().onAuthenticationSuccess(request,response,authentication);}).failureHandler((request,response,exception)->response.sendRedirect("/oauth-login?tenant="+URLEncoder.encode(Objects.toString(request.getParameter("tenant"),""),StandardCharsets.UTF_8)+"&error")).permitAll())
                .build();
    }

    @Bean AuthorizationServerSettings authorizationServerSettings(){return AuthorizationServerSettings.builder().multipleIssuersAllowed(true).authorizationEndpoint("/oauth2/authorize").tokenEndpoint("/oauth2/token").tokenRevocationEndpoint("/oauth2/revoke").jwkSetEndpoint("/oauth2/jwks").oidcUserInfoEndpoint("/userinfo").oidcLogoutEndpoint("/connect/logout").build();}
    @Bean JWKSource<SecurityContext> jwkSource(TenantSigningKeyService keys){return(selector,context)->selector.select(new JWKSet(keys.active(TenantIssuerContext.tenantId())));}
    @Bean JwtDecoder jwtDecoder(JWKSource<SecurityContext> source){return OAuth2AuthorizationServerConfiguration.jwtDecoder(source);}

    @Bean OAuth2TokenCustomizer<JwtEncodingContext> tokenClaims(PoolUserService users,DirectoryCatalogService directory,UserAccessCatalogService access){
        return context->{
            UUID tenant=TenantIssuerContext.tenantId();context.getClaims().audience(new ArrayList<>(List.of(context.getRegisteredClient().getClientId()))).claim("tenant_id",tenant.toString());
            if(AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())){context.getClaims().claim("subject_type","oauth_client");return;}
            try{
                UUID userId=UUID.fromString(context.getPrincipal().getName());var user=users.get(tenant,userId);
                context.getClaims().subject(userId.toString()).claim("subject_type","pool_user").claim("name",user.name()).claim("preferred_username",user.username()).claim("email",user.email()).claim("email_verified",user.emailVerified());
                if(user.phone()!=null)context.getClaims().claim("phone_number",user.phone()).claim("phone_number_verified",user.phoneVerified());
                if(user.avatar()!=null)context.getClaims().claim("picture",user.avatar());if(user.department()!=null)context.getClaims().claim("department",user.department());if(user.position()!=null)context.getClaims().claim("position",user.position());
                context.getClaims().claim("roles",new ArrayList<>(access.rolesForUser(tenant,userId).stream().map(UserAccessCatalogService.RoleView::code).toList())).claim("groups",new ArrayList<>(directory.groupsForUser(tenant,userId).stream().map(DirectoryCatalogService.GroupView::name).toList()));
            }catch(IllegalArgumentException ignored){}
        };
    }
    public record PoolLoginDetails(String tenant,String userAgent,String ip){}
}
