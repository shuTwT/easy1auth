package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.directory.DirectoryCatalogService;
import com.easy1auth.directory.PoolUserService;
import com.easy1auth.tenant.TenantContext;
import com.easy1auth.useraccess.UserAccessCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Phase4ControllerContractTest {
    private final UUID tenantId=UUID.randomUUID();
    private final PoolUserService users=mock(PoolUserService.class);
    private final DirectoryCatalogService directory=mock(DirectoryCatalogService.class);
    private final UserAccessCatalogService access=mock(UserAccessCatalogService.class);
    private MockMvc mvc;

    @BeforeEach void setUp(){
        mvc=MockMvcBuilders.standaloneSetup(
                new PoolUserController(users,directory,access),
                new DirectoryCatalogController(directory),
                new UserAccessCatalogController(access)
        ).build();
    }

    @Test void userListUsesStandardEnvelopeAndPaginationFields() throws Exception {
        when(users.list(eq(tenantId),eq(1),eq(10),isNull(),isNull(),isNull(),isNull(),isNull(),isNull()))
                .thenReturn(new PoolUserService.Page(List.of(),0,1,10));
        mvc.perform(get("/api/users").with(context()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray()).andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.page").value(1)).andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test void groupAndPositionListsUseStandardPagination() throws Exception {
        when(directory.groups(eq(tenantId),eq(1),eq(10),isNull(),isNull(),isNull())).thenReturn(new DirectoryCatalogService.Page<>(List.of(),0,1,10));
        when(directory.positions(eq(tenantId),eq(1),eq(10),isNull(),isNull(),isNull(),isNull())).thenReturn(new DirectoryCatalogService.Page<>(List.of(),0,1,10));
        mvc.perform(get("/api/groups").with(context())).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.items").isArray()).andExpect(jsonPath("$.data.pageSize").value(10));
        mvc.perform(get("/api/positions").with(context())).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.items").isArray()).andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test void rolesAndPermissionsUseTheSameEnvelope() throws Exception {
        when(access.roles(eq(tenantId),eq(1),eq(10),isNull(),isNull())).thenReturn(new UserAccessCatalogService.RolePage(List.of(),0,1,10));
        when(access.permissions(eq(tenantId),eq(1),eq(50),isNull(),isNull(),isNull())).thenReturn(new UserAccessCatalogService.PermissionPage(List.of(),0,1,50));
        mvc.perform(get("/api/roles").with(context())).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.items").isArray()).andExpect(jsonPath("$.data.pageSize").value(10));
        mvc.perform(get("/api/permissions").with(context())).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.items").isArray()).andExpect(jsonPath("$.data.pageSize").value(50));
    }

    private RequestPostProcessor context(){return request->{request.setAttribute(TenantContextFilter.ATTRIBUTE,new TenantContext(UUID.randomUUID(),tenantId,UUID.randomUUID(),"owner",Set.of("owner"),Set.of("*"),"trace"));return request;};}
}
