package org.carl.infrastructure.persistence.metadata;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * metadata 快照内部使用的标识符匹配规则。
 *
 * <p>这里显式区分 PostgreSQL quoted identifier 和 unquoted identifier，避免把
 * {@code user} 与 {@code "User"} 这类名字错误地折叠成同一个对象。
 */
final class MetadataNames {
    private static final Pattern UNQUOTED_IDENTIFIER =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");

    private MetadataNames() {}

    /** 按 PostgreSQL 标识符语义判断一个已存储名字是否命中给定查询名字。 */
    static boolean matches(String storedName, String lookupName) {
        if (storedName == null || lookupName == null) {
            return storedName == lookupName;
        }

        // Quoted PostgreSQL identifiers are case-sensitive and must match exactly.
        String quotedLookup = quotedIdentifierValue(lookupName);
        if (quotedLookup != null) {
            return storedName.equals(quotedLookup);
        }

        // Unquoted identifiers follow PostgreSQL's lower-case folding rules.
        String storedLookupKey = normalizeStoredName(storedName);
        String requestedLookupKey = normalizeLookupName(lookupName);
        return storedLookupKey != null && storedLookupKey.equals(requestedLookupKey);
    }

    /** 为数据库里原本就按大小写折叠的标识符生成 lookup key。 */
    static String normalizeStoredName(String value) {
        if (value == null) {
            return null;
        }

        if (!isCaseInsensitiveStoredName(value)) {
            return null;
        }

        return value.toLowerCase(Locale.ROOT);
    }

    /** 为调用方传入的非 quoted identifier 生成大小写无关的 lookup key。 */
    static String normalizeLookupName(String value) {
        if (value == null || isQuotedIdentifier(value) || !UNQUOTED_IDENTIFIER.matcher(value).matches()) {
            return null;
        }

        return value.toLowerCase(Locale.ROOT);
    }

    /** 从 quoted identifier 中提取数据库真正存储的名字。 */
    static String quotedIdentifierValue(String value) {
        if (!isQuotedIdentifier(value)) {
            return null;
        }
        return value.substring(1, value.length() - 1).replace("\"\"", "\"");
    }

    private static boolean isQuotedIdentifier(String value) {
        return value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"");
    }

    private static boolean isCaseInsensitiveStoredName(String value) {
        return value.equals(value.toLowerCase(Locale.ROOT))
                && UNQUOTED_IDENTIFIER.matcher(value).matches();
    }
}
