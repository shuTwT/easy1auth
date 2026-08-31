package com.easy1auth.connection.dto;
import java.util.UUID;
/** 待确认绑定的远程身份。 */
public record PendingIdentity(UUID tenantId, UUID sourceId, String sourceType, String subject,
                              String username, String name, String email, String avatar) implements java.io.Serializable { }
