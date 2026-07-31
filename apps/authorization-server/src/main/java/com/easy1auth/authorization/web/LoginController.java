package com.easy1auth.authorization.web;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import com.easy1auth.security.SecurityPolicyService;
import com.easy1auth.federation.FederationService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class LoginController {
    private final SecurityPolicyService security; private final FederationService federation;
    LoginController(SecurityPolicyService security,FederationService federation){this.security=security;this.federation=federation;}
    @GetMapping(value="/oauth-login",produces=MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> login(@RequestParam(required=false)String tenant,@RequestParam(required=false)String error,HttpServletRequest request){
        try{UUID.fromString(tenant);}catch(RuntimeException ex){return ResponseEntity.badRequest().body("Invalid tenant");}
        CsrfToken csrf=(CsrfToken)request.getAttribute(CsrfToken.class.getName());String csrfInput=csrf==null?"":"<input type=\"hidden\" name=\""+escape(csrf.getParameterName())+"\" value=\""+escape(csrf.getToken())+"\">";
        String message=error==null?"":"<p role=\"alert\">用户名或密码错误</p>";
        return ResponseEntity.ok("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><title>Easy1Auth 登录</title></head><body><main><h1>登录</h1>"+message+"<form method=\"post\" action=\"/oauth-login\">"+csrfInput+"<input type=\"hidden\" name=\"tenant\" value=\""+escape(tenant)+"\"><label>用户名或邮箱<input name=\"username\" autocomplete=\"username\" required></label><label>密码<input type=\"password\" name=\"password\" autocomplete=\"current-password\" required></label><button type=\"submit\">登录</button></form></main></body></html>");
    }
    @GetMapping(value="/oauth-login/mfa",produces=MediaType.TEXT_HTML_VALUE) ResponseEntity<String> mfa(HttpServletRequest request){if(request.getSession(false)==null||request.getSession().getAttribute("EASY1AUTH_MFA_CHALLENGE")==null)return ResponseEntity.status(302).header("Location","/oauth-login").build();CsrfToken csrf=(CsrfToken)request.getAttribute(CsrfToken.class.getName());String hidden=csrf==null?"":"<input type=\"hidden\" name=\""+escape(csrf.getParameterName())+"\" value=\""+escape(csrf.getToken())+"\">";return ResponseEntity.ok("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><title>MFA 验证</title></head><body><main><h1>多因素认证</h1><form method=\"post\" action=\"/oauth-login/mfa\">"+hidden+"<label>动态验证码<input name=\"code\" inputmode=\"numeric\" autocomplete=\"one-time-code\" required></label><button type=\"submit\">验证</button></form></main></body></html>");}
    @PostMapping("/oauth-login/mfa") void verifyMfa(@RequestParam String code,HttpServletRequest request,HttpServletResponse response)throws java.io.IOException{var session=request.getSession(false);if(session==null){response.sendRedirect("/oauth-login");return;}String challenge=(String)session.getAttribute("EASY1AUTH_MFA_CHALLENGE");UUID expected=(UUID)session.getAttribute("EASY1AUTH_MFA_USER"),tenant=(UUID)session.getAttribute("EASY1AUTH_MFA_TENANT");UUID actual=security.consumeTotpChallenge(challenge,code);if(!actual.equals(expected)){session.invalidate();response.sendRedirect("/oauth-login?error");return;}var authorities=java.util.List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"),new SimpleGrantedAuthority("TENANT_"+tenant),new SimpleGrantedAuthority("MFA_VERIFIED"));var auth=UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(actual.toString()).password("").authorities(authorities).build(),null,authorities);SecurityContext context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,request,response);session.removeAttribute("EASY1AUTH_MFA_CHALLENGE");session.removeAttribute("EASY1AUTH_MFA_USER");session.removeAttribute("EASY1AUTH_MFA_TENANT");var saved=new HttpSessionRequestCache().getRequest(request,response);response.sendRedirect(saved==null?"/":saved.getRedirectUrl());}
    @GetMapping("/t/{tenant}/federation/{provider}/authorize") void federationStart(@PathVariable UUID tenant,@PathVariable UUID provider,@RequestParam String redirectUri,HttpServletResponse response)throws java.io.IOException{response.sendRedirect(federation.authorize(tenant,provider,redirectUri).authorizeUrl());}
    @GetMapping("/t/{tenant}/federation/{provider}/callback") void federationCallback(@PathVariable UUID tenant,@PathVariable UUID provider,@RequestParam String code,@RequestParam String state,HttpServletRequest request,HttpServletResponse response)throws java.io.IOException{String callback=request.getRequestURL().toString();var result=federation.callback(tenant,provider,code,state,callback);var authorities=java.util.List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"),new SimpleGrantedAuthority("TENANT_"+tenant));var auth=UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(result.poolUserId().toString()).password("").authorities(authorities).build(),null,authorities);SecurityContext context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,request,response);var saved=new HttpSessionRequestCache().getRequest(request,response);response.sendRedirect(saved==null?"/":saved.getRedirectUrl());}
    private static String escape(String value){return value==null?"":value.replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;");}
}
