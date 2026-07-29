package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.directory.PoolUserService;
import com.easy1auth.directory.PoolUserView;
import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PoolUserImportControllerTest {
    private final PoolUserService users=mock(PoolUserService.class);
    private final PoolUserImportController controller=new PoolUserImportController(users);

    @Test void templateContainsTheLegacySevenColumns() throws Exception {
        byte[] bytes=controller.template().getBody();
        assertThat(bytes).isNotNull();
        try(var workbook=WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var row=workbook.getSheetAt(0).getRow(0);
            assertThat(row.getLastCellNum()).isEqualTo((short)7);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("用户名");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("邮箱");
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("岗位");
        }
    }

    @Test void uploadParsesRowsAndReturnsPerRowErrors() throws Exception {
        UUID tenant=UUID.randomUUID(); var request=mock(HttpServletRequest.class);
        when(request.getAttribute(TenantContextFilter.ATTRIBUTE)).thenReturn(new TenantContext(UUID.randomUUID(),tenant,UUID.randomUUID(),"owner",Set.of(),Set.of(),"trace"));
        when(users.create(eq(tenant),any())).thenAnswer(invocation->{
            PoolUserService.Input input=invocation.getArgument(1);
            if(input.username().equals("broken")) throw new IllegalStateException("duplicate");
            Instant now=Instant.now();
            return new PoolUserView(UUID.randomUUID(),tenant,input.username(),input.email(),input.phone(),input.name(),null,"active",false,false,input.department(),input.position(),null,null,now,now);
        });

        byte[] workbookBytes;
        try(var workbook=new XSSFWorkbook();var output=new ByteArrayOutputStream()) {
            var sheet=workbook.createSheet("users");
            var header=sheet.createRow(0); header.createCell(0).setCellValue("用户名"); header.createCell(1).setCellValue("邮箱"); header.createCell(4).setCellValue("姓名");
            var valid=sheet.createRow(1); valid.createCell(0).setCellValue("alice"); valid.createCell(1).setCellValue("alice@example.com"); valid.createCell(2).setCellValue("Password1"); valid.createCell(4).setCellValue("Alice"); valid.createCell(5).setCellValue("研发"); valid.createCell(6).setCellValue("工程师");
            var invalid=sheet.createRow(2); invalid.createCell(0).setCellValue("broken"); invalid.createCell(1).setCellValue("broken@example.com"); invalid.createCell(4).setCellValue("Broken");
            workbook.write(output); workbookBytes=output.toByteArray();
        }

        var file=new MockMultipartFile("file","users.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",workbookBytes);
        Map<String,Object> response=controller.upload(request,file);
        assertThat(response.get("status")).isEqualTo("success");
        @SuppressWarnings("unchecked") var data=(Map<String,Object>)response.get("data");
        assertThat(data).containsEntry("success",1).containsEntry("failed",1).containsEntry("total",2);
        assertThat((java.util.List<?>)data.get("errors")).singleElement().extracting("row").isEqualTo(3);
    }
}
