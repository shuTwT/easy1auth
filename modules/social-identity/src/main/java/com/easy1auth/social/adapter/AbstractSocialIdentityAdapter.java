package com.easy1auth.social.adapter;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.social.constant.ErrorCodeConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 适配器共享的 HTTP 工具基类：提供表单 POST、JSON POST、GET 与编码辅助方法。
 */
abstract class AbstractSocialIdentityAdapter implements SocialIdentityAdapter {

    /** 共享的 HTTP 客户端：8 秒连接超时、不自动跟随重定向 */
    protected static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** JSON 序列化器 */
    protected final ObjectMapper json;

    protected AbstractSocialIdentityAdapter(ObjectMapper json) {
        this.json = json;
    }

    /** 发送 application/x-www-form-urlencoded 表单 POST，返回解析后的 JSON。 */
    protected Map<String, Object> postForm(String url, Map<String, String> form, String authorization) {
        try {
            var body = new StringBuilder();
            for (var e : form.entrySet()) {
                if (body.length() > 0) {
                    body.append('&');
                }
                body.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
            }
            var builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json");
            if (authorization != null) {
                builder.header("Authorization", authorization);
            }
            var req = builder.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200 || res.body().length() > 1_000_000) {
                throw new IOException();
            }
            return json.readValue(res.body(), new TypeReference<>() { });
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
    }

    /** 发送 application/json POST，返回解析后的 JSON。 */
    protected Map<String, Object> postJson(String url, Object body) {
        try {
            var req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200 || res.body().length() > 1_000_000) {
                throw new IOException();
            }
            return json.readValue(res.body(), new TypeReference<>() { });
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
    }

    /** 发送带 Bearer token 的 GET，返回解析后的 JSON。 */
    protected Map<String, Object> getJson(String url, String bearer) {
        try {
            var req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + bearer)
                    .GET().build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200 || res.body().length() > 1_000_000) {
                throw new IOException();
            }
            return json.readValue(res.body(), new TypeReference<>() { });
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_USERINFO_FAILED);
        }
    }

    /** URL 编码（UTF-8），用于拼接 query 参数。 */
    protected static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** 空值安全地转为字符串，供取值后统一处理。 */
    protected static String string(Object o) {
        return o == null ? null : o.toString();
    }

    /** 去掉自身 IOException 声明，便于 lambda 抛出。 */
    @SuppressWarnings("serial")
    private static class IOException extends Exception { }
}
