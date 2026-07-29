package com.easy1auth.authorization.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IssuerHostValidationFilterTest {
    @Test void rejectsHostHeaderIssuerConfusion()throws Exception{
        var filter=new IssuerHostValidationFilter("https://auth.example.com");var request=new MockHttpServletRequest("GET","/t/"+UUID.randomUUID()+"/oauth2/token");request.setScheme("https");request.setServerName("evil.example");request.setServerPort(443);var response=new MockHttpServletResponse();var chain=mock(FilterChain.class);
        filter.doFilter(request,response,chain);assertThat(response.getStatus()).isEqualTo(400);verifyNoInteractions(chain);
    }
    @Test void acceptsConfiguredOriginAndUuidTenant()throws Exception{
        var filter=new IssuerHostValidationFilter("https://auth.example.com");var request=new MockHttpServletRequest("GET","/t/"+UUID.randomUUID()+"/oauth2/token");request.setScheme("https");request.setServerName("auth.example.com");request.setServerPort(443);var response=new MockHttpServletResponse();var chain=mock(FilterChain.class);
        filter.doFilter(request,response,chain);verify(chain).doFilter(request,response);
    }
}
