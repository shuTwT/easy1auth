package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.poolidentity.service.PoolUserService;
import com.easy1auth.poolidentity.dto.PoolUserInput;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

/**
 * 目录用户批量导入接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/users/import}，提供 Excel 导入模板下载
 * 与批量导入目录用户（pool_user）的能力。导入时逐行校验，返回成功与失败的明细。
 * 操作通过 {@link TenantManagementPermission} 做租户级权限控制，并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/users/import")
public class PoolUserImportController {
    /** 导入文件允许的最大体积（5MB） */
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    /** 目录用户服务 */
    private final PoolUserService users;

    PoolUserImportController(PoolUserService users) {
        this.users = users;
    }

    /** 下载用户导入用的 Excel 模板文件。 */
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

    /** 上传 Excel 文件批量导入目录用户，返回成功与失败的行级明细。 */
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
                    var user = users.create(new PoolUserInput(username, email, blank(password), blank(phone), name, null, null, blank(department), blank(position), null));
                    imported.add(new ImportedUser(user.username(), user.email(), user.name()));
                } catch (RuntimeException ex) {
                    errors.add(new ImportError(rowIndex + 1, username,
                            ex instanceof DomainException ? ex.getMessage() : "导入失败或数据重复"));
                }
            }
        }
        return ApiResponse.ok(new ImportResponse(imported.size(), errors.size(), total, errors, imported), "导入完成：成功 " + imported.size() + " 条，失败 " + errors.size() + " 条");
    }

    /** 读取表格中指定列单元格的字符串值，缺失或空返回空串。 */
    private static String value(Row row, int index, DataFormatter formatter) {
        var cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).strip();
    }

    /** 把空白字符串归一化为 null，便于按可选字段处理。 */
    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** 用于判断一行数据是否全部为空的工具类。 */
    private static final class StreamSupport {
        static boolean empty(String... values) {
            return Arrays.stream(values).allMatch(String::isBlank);
        }
    }

    /**
     * 导入失败的行错误信息。
     *
     * @param row      出错行号（Excel 行号，从 1 开始）
     * @param username 该行对应的用户名
     * @param error    失败原因描述
     */
    public record ImportError(int row, String username, String error) {
    }

    /**
     * 成功导入的用户信息。
     *
     * @param username 用户名
     * @param email    邮箱
     * @param name     姓名
     */
    public record ImportedUser(String username, String email, String name) {
    }

    /**
     * 导入结果汇总。
     *
     * @param success       成功导入的条数
     * @param failed        失败条数
     * @param total         参与导入的总条数（非空行）
     * @param errors        失败行的错误明细
     * @param importedUsers 成功导入的用户明细
     */
    public record ImportResponse(int success, int failed, int total, List<ImportError> errors,
                                 List<ImportedUser> importedUsers) {
    }
}
