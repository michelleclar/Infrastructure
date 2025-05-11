dependencies {
    api(project(":infrastructure-components:infrastructure-component-tool"))
    api(libs.bundles.persistence)
    api("com.querydsl:querydsl-apt:5.1.0")  // QueryDSL 注解处理器
    api("com.querydsl:querydsl-jpa:5.1.0")  // 如果不需要JPA，可以不加这个
    api("com.querydsl:querydsl-sql:5.1.0")  // 用于数据库查询构建
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jpa")
}
