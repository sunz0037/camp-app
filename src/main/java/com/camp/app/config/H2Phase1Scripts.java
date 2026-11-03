package com.camp.app.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * H2 不能执行 MySQL 的 PREPARE / SET @sql := IF。
 * 只在 dev 启动时改写语句，不改仓根 SQL 文件。
 */
public final class H2Phase1Scripts {
    private static final Pattern PREPARE_BLOCK = Pattern.compile(
            "SET\\s+@\\w+\\s*:=[\\s\\S]*?DEALLOCATE\\s+PREPARE\\s+stmt\\s*;",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_NAMES = Pattern.compile("SET\\s+NAMES\\s+utf8mb4\\s*;", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENGINE = Pattern.compile("\\s*ENGINE\\s*=\\s*InnoDB", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHARSET = Pattern.compile("\\s*DEFAULT\\s+CHARSET\\s*=\\s*utf8mb4", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERATED = Pattern.compile(
            "(\\w+)\\s+BIGINT\\s+AS\\s+\\((.+?)\\)\\s+STORED",
            Pattern.CASE_INSENSITIVE);

    private H2Phase1Scripts() {
    }

    public static String adapt(String mysql) {
        String sql = SET_NAMES.matcher(mysql).replaceAll("");
        sql = PREPARE_BLOCK.matcher(sql).replaceAll("");
        sql = GENERATED.matcher(sql).replaceAll("$1 BIGINT GENERATED ALWAYS AS ($2)");
        sql = ENGINE.matcher(sql).replaceAll("");
        sql = CHARSET.matcher(sql).replaceAll("");
        return prefixIndexNames(sql);
    }

    public static String adaptSeed(String mysql) {
        Matcher password = Pattern.compile("SET\\s+@demo_password\\s*:=\\s*'([^']*)'\\s*;", Pattern.CASE_INSENSITIVE)
                .matcher(mysql);
        if (!password.find()) {
            return mysql;
        }
        String hashLiteral = "'" + password.group(1) + "'";
        String withoutSet = password.replaceFirst("");
        String sql = withoutSet.replace("@demo_password", hashLiteral);
        return sql.replaceAll(
                "(?is)UPDATE\\s+emergency_contacts\\s+ec\\s+JOIN\\s+students\\s+s\\s+ON\\s+s\\.id\\s*=\\s*ec\\.student_id\\s+AND\\s+s\\.student_no\\s*=\\s*'([^']+)'\\s+SET\\s+ec\\.is_primary\\s*=\\s*1\\s*,\\s*ec\\.relationship\\s*=\\s*'([^']*)'\\s+WHERE\\s+ec\\.contact_name\\s*=\\s*'([^']*)'\\s+AND\\s+ec\\.phone\\s*=\\s*'([^']*)'\\s*;",
                "UPDATE emergency_contacts SET is_primary = 1, relationship = '$2' WHERE contact_name = '$3' AND phone = '$4' AND student_id IN (SELECT id FROM students WHERE student_no = '$1');");
    }

    /**
     * H2 的索引名是库级的，MySQL 可以多张表共用 idx_status。
     */
    static String prefixIndexNames(String sql) {
        Pattern create = Pattern.compile("CREATE TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);
        Matcher matcher = create.matcher(sql);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            int statementEnd = sql.indexOf(';', matcher.end());
            if (statementEnd < 0) {
                break;
            }
            out.append(sql, last, matcher.end());
            String table = matcher.group(1);
            String body = sql.substring(matcher.end(), statementEnd);
            body = body.replaceAll("(?i)UNIQUE\\s+KEY\\s+(\\w+)", "UNIQUE KEY " + table + "_$1");
            body = body.replaceAll("(?i)(?<!PRIMARY )(?<!UNIQUE )\\bKEY\\s+(\\w+)", "KEY " + table + "_$1");
            body = body.replaceAll("(?i)CONSTRAINT\\s+(\\w+)", "CONSTRAINT " + table + "_$1");
            out.append(body);
            last = statementEnd;
        }
        out.append(sql.substring(last));
        return out.toString();
    }
}
