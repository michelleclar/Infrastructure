package org.carl.infrastructure.persistence.metadata;

/**
 * 数据库表上的 check constraint 快照。
 *
 * <p>这里只保留约束名和数据库返回的定义文本，不承载任何加载逻辑。
 */
@Deprecated
public class DBCheckConstraint {
    private final String name;
    private final String definition;

    public DBCheckConstraint(String name, String definition) {
        this.name = name;
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public String getDefinition() {
        return definition;
    }
}
