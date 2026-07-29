package com.easy1auth.authorization.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantPrincipalValidationFilterTest {
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void rejectsPoolUserAuthenticatedForAnotherTenant()throws Exception{
        UUID selected=UUID.randomUUID(),authenticated=UUID.randomUUID();var auth=UsernamePasswordAuthenticationToken.authenticated("user",null,List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"),new SimpleGrantedAuthority("TENANT_"+authenticated)));SecurityContextHolder.getContext().setAuthentication(auth);
        var request=new MockHttpServletRequest("GET","/t/"+selected+"/oauth2/authorize");var response=new MockHttpServletResponse();var chain=mock(FilterChain.class);
        new TenantPrincipalValidationFilter().doFilter(request,response,chain);assertThat(response.getStatus()).isEqualTo(403);verifyNoInteractions(chain);
    }
}
