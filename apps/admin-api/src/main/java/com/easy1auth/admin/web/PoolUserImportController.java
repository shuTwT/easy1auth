package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.directory.PoolUserService;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.web.ApiResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api/users/import")
public class PoolUserImportController {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private final PoolUserService users;

    PoolUserImportController(PoolUserService users) {
        this.users = users;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_IMPORT_TEMPLATE)
    @GetMapping("/template")
    ResponseEntity<byte[]> template() throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("用户导入");
            var header = sheet.createRow(0);
            String[] columns = {"用户名", "邮箱", "密码", "手机号", "姓名", "部门", "岗位"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(output);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_import_template.xlsx").body(output.toByteArray());
        }
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_IMPORT)
    @PostMapping
    ApiResponse<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new DomainException(ErrorCodeConstants.IMPORT_FILE_INVALID);
        }
        List<ImportError> errors = new ArrayList<>();
        List<ImportedUser> imported = new ArrayList<>();
        int total = 0;
        try (var input = new BufferedInputStream(file.getInputStream()); var workbook = WorkbookFactory.create(input)) {
            var sheet = workbook.getSheetAt(0);
            var formatter = new DataFormatter();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                var row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String username = value(row, 0, formatter), email = value(row, 1, formatter), password = value(row, 2, formatter), phone = value(row, 3, formatter), name = value(row, 4, formatter), department = value(row, 5, formatter), position = value(row, 6, formatter);
                if (StreamSupport.empty(username, email, name)) {
                    continue;
                }
                total++;
                try {
                    var user = users.create(new PoolUserService.Input(username, email, blank(password), blank(phone), name, null, null, blank(department), blank(position), null));
                    imported.add(new ImportedUser(user.username(), user.email(), user.name()));
                } catch (RuntimeException ex) {
                    errors.add(new ImportError(rowIndex + 1, username,
                            ex instanceof DomainException ? ex.getMessage() : "导入失败或数据重复"));
                }
            }
        }
        return ApiResponse.ok(new ImportResponse(imported.size(), errors.size(), total, errors, imported), "导入完成：成功 " + imported.size() + " 条，失败 " + errors.size() + " 条");
    }

    private static String value(Row row, int index, DataFormatter formatter) {
        var cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).strip();
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static final class StreamSupport {
        static boolean empty(String... values) {
            return Arrays.stream(values).allMatch(String::isBlank);
        }
    }

    public record ImportError(int row, String username, String error) {
    }

    public record ImportedUser(String username, String email, String name) {
    }

    public record ImportResponse(int success, int failed, int total, List<ImportError> errors,
                                 List<ImportedUser> importedUsers) {
    }
}
