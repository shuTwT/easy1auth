package com.easy1auth.oauth2;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component
public class TenantOAuth2AuthorizationService implements OAuth2AuthorizationService {
    private final JdbcOAuth2AuthorizationService delegate;
    private final RegisteredClientRepository clients;
    private final JdbcOperations jdbc;
    public TenantOAuth2AuthorizationService(JdbcOperations jdbc,RegisteredClientRepository clients){this.delegate=new JdbcOAuth2AuthorizationService(jdbc,clients);this.clients=clients;this.jdbc=jdbc;}
    @Override public void save(OAuth2Authorization authorization){requireClient(authorization.getRegisteredClientId());delegate.save(authorization);}
    @Override public void remove(OAuth2Authorization authorization){requireClient(authorization.getRegisteredClientId());delegate.remove(authorization);}
    @Override public OAuth2Authorization findById(String id){return belongs("select tenant_id from oauth2_authorization where id=?",id)?valid(delegate.findById(id)):null;}
    @Override public OAuth2Authorization findByToken(String token,OAuth2TokenType tokenType){return belongs("select tenant_id from oauth2_authorization where state=? or authorization_code_value=? or access_token_value=? or refresh_token_value=? or oidc_id_token_value=? or user_code_value=? or device_code_value=?",token,token,token,token,token,token,token)?valid(delegate.findByToken(token,tokenType)):null;}
    private OAuth2Authorization valid(OAuth2Authorization value){if(value==null)return null;requireClient(value.getRegisteredClientId());return value;}
    private void requireClient(String id){if(clients.findById(id)==null)throw new IllegalArgumentException("OAuth authorization does not belong to requested tenant");}
    private boolean belongs(String statement,Object...params){var tenants=jdbc.query(statement,(rs,row)->rs.getObject(1,java.util.UUID.class),params);return !tenants.isEmpty()&&tenants.stream().allMatch(TenantIssuerContext.tenantId()::equals);}
}
