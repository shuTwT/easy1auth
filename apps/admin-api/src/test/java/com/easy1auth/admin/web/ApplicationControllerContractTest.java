package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.application.ApplicationService;
import com.easy1auth.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApplicationControllerContractTest {
    @Test void listKeepsVueEnvelopeAndNeverReturnsStoredSecret()throws Exception{
        UUID tenant=UUID.randomUUID();var service=mock(ApplicationService.class);when(service.list(eq(tenant),eq(1),eq(10),isNull(),isNull(),isNull())).thenReturn(new ApplicationService.ApplicationPage(List.of(),0,1,10));
        var mvc=MockMvcBuilders.standaloneSetup(new ApplicationController(service)).build();
        mvc.perform(get("/api/applications").requestAttr(TenantContextFilter.ATTRIBUTE,new TenantContext(UUID.randomUUID(),tenant,UUID.randomUUID(),"owner",Set.of(),Set.of(),"trace")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success")).andExpect(jsonPath("$.data.applications").isArray()).andExpect(jsonPath("$.data.pageSize").value(10));
    }
}
