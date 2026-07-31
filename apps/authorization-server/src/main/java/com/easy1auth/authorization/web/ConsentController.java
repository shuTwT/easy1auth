package com.easy1auth.authorization.web;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.*;

@Controller
public class ConsentController {
    private final JdbcClient db;

    ConsentController(JdbcClient db) { this.db = db; }

    @GetMapping(value="/oauth-consent",produces=MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> consent(@RequestParam("client_id")String clientId,@RequestParam(required=false)String scope,@RequestParam(required=false)String state,HttpServletRequest request){
        var tenant = db.sql("select tenant_id from oauth_application where client_id=:clientId and status='active'")
                .param("clientId", clientId).query(UUID.class).optional();
        if (tenant.isEmpty()) return ResponseEntity.badRequest().body("Invalid client");
        CsrfToken csrf=(CsrfToken)request.getAttribute(CsrfToken.class.getName());StringBuilder scopes=new StringBuilder();
        if(scope!=null)for(String value:scope.split(" "))if(!value.isBlank())scopes.append("<label><input type=\"checkbox\" name=\"scope\" value=\"").append(escape(value)).append("\" checked>").append(escape(value)).append("</label>");
        String csrfInput=csrf==null?"":"<input type=\"hidden\" name=\""+escape(csrf.getParameterName())+"\" value=\""+escape(csrf.getToken())+"\">";
        return ResponseEntity.ok("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><title>授权确认</title></head><body><main><h1>授权确认</h1><p>客户端："+escape(clientId)+"</p><form method=\"post\" action=\"/t/"+tenant.get()+"/oauth2/authorize\">"+csrfInput+"<input type=\"hidden\" name=\"client_id\" value=\""+escape(clientId)+"\"><input type=\"hidden\" name=\"state\" value=\""+escape(state)+"\">"+scopes+"<button type=\"submit\">同意</button><button type=\"submit\" name=\"consent_action\" value=\"cancel\">拒绝</button></form></main></body></html>");
    }
    private static String escape(String value){return value==null?"":value.replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;");}
}
