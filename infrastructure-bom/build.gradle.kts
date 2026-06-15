plugins {
    `java-platform`
    `maven-publish`
}

javaPlatform {
    // 允许 api(platform(...)) 导入另一个 BOM（默认 java-platform 禁止声明依赖）
    allowDependencies()
}

dependencies {
    // 重新导出 Quarkus 平台 BOM，使消费方编译期+运行期都带约束
    api(platform(libs.quarkus.platform.bom))

    constraints {
        // 强制 quarkus-bom 版本：被消费方当作 enforcedPlatform 引入时，此约束会被提升为 enforced，
        // 从而压平各 infra 组件里历史遗留、互相冲突的 strictly quarkus-bom 版本（如旧的 3.28.3）。
        api(libs.quarkus.platform.bom)
        // 约束消费方实际使用的 org.carl 组件（artifactId 以当前 infra 发布产物为准）
        api("org.carl:infrastructure-component-dto:${version}")
        api("org.carl:infrastructure-component-quarkus-authorization:${version}")
        api("org.carl:infrastructure-component-quarkus-persistence:${version}")
        api("org.carl:infrastructure-component-utils:${version}")
        api("org.carl:infrastructure-component-quarkus-web:${version}")
        api("org.carl:infrastructure-component-quarkus-mq:${version}")
        // 注意：不约束 postgresql —— 现状下其版本已被 enforced quarkus-bom 实际接管，
        // 单独约束会改变既有解析结果，故保持行为不变。
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["javaPlatform"])
            groupId = project.group.toString()
            artifactId = "infrastructure-bom"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            credentials {
                username = System.getenv("ALIYUN_MAVEN_USERNAME") ?: ""
                password = System.getenv("ALIYUN_MAVEN_PASSWORD") ?: ""
            }
            url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
        }
    }
}
