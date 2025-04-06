org.gradle.caching=(true,false)
构建缓存配置。当设置为 true 时，Gradle会尽可能复用任何之前构建的任务输出，可以大大提高构建速度。默认值为 false（禁用构建缓存）。

org.gradle.caching.debug=(true,false)
构建缓存调试开关。当设置为 true 时，部分输入属性的哈希值和构建缓存中的每一个任务的缓存 key 将会在控制台中输出。默认为 false。

org.gradle.configureondemand=(true,false)
启用按需配置，如果设置为 true，Gradle只会尝试配置必须的项目。默认为 false。

org.gradle.console=(auto,plain,rich,verbose)
控制台显示配置，用于自定义控制台输出文字样式（彩色或者黑白等）。默认值取决于Gradle唤起方式。

org.gradle.continuous.quietperiod=(# of quiet period millis)
连续构建静默等待时间配置。在使用连续性构建中，Gradle会在触发另一个构建之前静默等待一段时间，在静默等待时间内任何额外的改变，都将会重置静默等待时间。默认值是250毫秒。

org.gradle.daemon=(true,false)
启用守护进程，如果设置为 true，Gradle将会启动守护进程运行构建，默认值为 true。

org.gradle.daemon.idletimeout=(# of idle millis)
守护进程空闲时间上限，如果Gradle守护进程空闲时间超过这个上限，守护进程将会自己关闭。默认值是 10800000 毫秒（3小时）。

org.gradle.debug=(true,false)
开启调试模式，如果设置为 true，Gradle将会启用远程调试模式运行构建，监听端口为5005，默认值为 false。需要注意的是，这个属性配置跟在 JVM 命令行中添加 -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 具有同等效果，并且在调试器附加完成之前，JVM虚拟机将处于暂停状态。

org.gradle.debug.port=(port number)
指定调试器监听端口，仅在开启调试模式时生效，默认值是 5005。

org.gradle.debug.suspend=(true,false)
附加调试器时 JVM 虚拟机是否暂停。当设置为 true 并且启用了调试模式，在调试器附加完成之前，JVM虚拟机将会暂停（挂起），默认值是 true。

org.gradle.java.home=(path to JDK home)
指定Java根目录。为Gradle构建进程指定一个Java主目录，这个值可以设置为 JDK 或者 jar 所在的目录路径（取决于你构建的内容，使用JDK更安全）。这个属性配置不会影响启动 Gradle 客户端VM的 Java 版本（仅影响当前的 Gradle 构建）。如果没有指定，正常情况下默认值是在系统环境变量中 JAVA_HOME 指定或者指向 Java 的路径。

org.gradle.jvmargs=(JVM arguments)
设置 JVM参数。指定用于 Gradle 守护进程使用的 JVM 参数。这个参数设置仅仅作用与 Gradle 构建的 JVM 内存配置，并不会影响 Gradle 客户端运行的 JVM 设置。默认值是 -Xmx512m "-XX:MaxMetaspaceSize=256m"。

rg.gradle.logging.level=(quiet,warn,lifecycle,info,debug)
指定日志输出级别。Gradle会使用指定的日志输出级别，不同的级别将会影响日志输出结果和详细情况。默认值是 lifecycle。

org.gradle.parallel=(true,false)
是否开启并行构建。如果设置为 true，Gradle 将会以 org.gradle.workers.max 参数指定数量的 JVM 并行执行项目的并行构建。默认值是 false。

org.gradle.priority=(low,normal)
指定 Gradle 守护进程极其启动的所有进程的优先级。默认值是 normal。

org.gradle.vfs.verbose=(true,false)
配置监视文件系统时是否记录详细日志信息。默认值是 false。

org.gradle.vfs.watch=(true,false)
是否开启监视文件系统。如果开启（设置为 true），Gradle 在构建多个项目之间会重复使用收集到的文件系统信息。默认值是 false。

org.gradle.warning.mode=(all,fail,summary,none)
设置 Gradle 警告展示级别，Gradle 会根据不同值展示不同级别类型的警告信息。默认值是 summary。

org.gradle.workers.max=(max # of worker processes)
设置 Gradle 最大工作线程数，设置之后，Gradle 会根据给定的最大工作线程数并行构建项目。默认值是当前设备 CPU 的线程数。

org.gradle.logging.stacktrace=(internal,all,full)
指定异常堆栈输出信息级别，Gradle 会根据设置的级别在输出信息中显示异常堆栈信息的详细情况（跟命令行的 --stacktrace 选项具有相同效果）。设置为 internal，只有内部的异常堆栈信息才会输出到控制台。如果设置成 all 或者 full，控制台会输出所有的异常堆栈信息和构建失败信息，使用 full 是控制台不会截断异常堆栈信息，将会得到更详细的输出。默认值是 internal。

org.gradle.welcome=(never,once)
控制 Gradle 是否需要输出欢迎信息。如果设置为 never，不会打印欢迎信息，如果设置为 once，每一个新版本的 Gradle 启动执行时只会打印一次欢迎信息（以后不会再打印输出）。默认值是 once。

依赖配置	Compile Classpath	Runtime Classpath
api	√	√
implementation	√	√
compileOnlyApi	√	×
compileOnly	√	×
runtimeOnly	×	√
testImplementation	√	√
testCompileOnly	√	×
testRuntimeOnly	×	√
annotationProcessor	√	×
//dependencyResolutionManagement {
//    versionCatalogs {
//        libs {
//            // versions
//            version('quarkus', "${quarkusPluginVersion}")
//            version('jooq', '3.19.12')
//            version('testcontainers', '1.20.4')
//            version('consul', '2.2.2')
//            version('hanlp', 'portable-1.8.4')
//            version('commons', '4.4')
//            version('guava', '33.2.1-jre')
//
//            // NOTE: quarkus
//            plugin('quarkusPlugin', 'io.quarkus').versionRef('quarkus')
//            library('quarkusPlatform', 'io.quarkus.platform', 'quarkus-bom').versionRef('quarkus')
//            library('quarkusCamelPlatform', 'io.quarkus.platform', 'quarkus-camel-bom').versionRef('quarkus')
//            library('quarkusSmallryeOpenapi', 'io.quarkus', 'quarkus-smallrye-openapi').versionRef('quarkus')
//            library('quarkusArc', 'io.quarkus', 'quarkus-arc').versionRef('quarkus') //CDI
//            library('configYaml', 'io.quarkus', 'quarkus-config-yaml').versionRef('quarkus') //CDI
//            library('rest', 'io.quarkus', 'quarkus-rest').versionRef('quarkus')
//            library('routes', 'io.quarkus', 'quarkus-reactive-routes').versionRef('quarkus')
//            library('vertx', 'io.quarkus', 'quarkus-vertx').versionRef('quarkus')
//            library('jsonp', 'io.quarkus', 'quarkus-jsonp').versionRef('quarkus')
//            library('jackson', 'io.quarkus', 'quarkus-rest-jackson').versionRef('quarkus')
//
//            library('context', 'io.quarkus', 'quarkus-smallrye-context-propagation').versionRef('quarkus')
//            library('xml', 'jakarta.xml.bind', 'jakarta.xml.bind-api').version('4.0.2')
//
//            // NOTE: protocol
//            library('websocket', 'io.quarkus', 'quarkus-websockets-client').versionRef('quarkus')
//            // NOTE: auth
//            library('oidc', 'io.quarkus', 'quarkus-oidc').versionRef('quarkus')
//            library('keycloak', 'io.quarkus', 'quarkus-keycloak-authorization').versionRef('quarkus')
//            library('vault', 'io.quarkiverse', 'quarkus-vault').versionRef('quarkus')
//            // NOTE: cached
//            library('cache', 'io.quarkus', 'quarkus-cache').versionRef('quarkus')
//
//            // NOTE: slf4j-jboss-logmanager (links quarkus jboss-logging so it can log jOOQs slf4j logging)
//            //            library('slf4jJbossLogManager', 'org.jboss.slf4j', 'slf4j-jboss-logmanager').versionRef('slf4j-jboss-logmanager')
//
//            // NOTE: vert.x
//            library('vertxClient', 'io.smallrye.reactive', 'smallrye-mutiny-vertx-web-client').versionRef('quarkus')
//
//            // NOTE: db
//            library('agroal', 'io.quarkus', 'quarkus-agroal').versionRef('quarkus')
//            library('quarkusJdbcPostgreSQL', 'io.quarkus', 'quarkus-jdbc-postgresql').versionRef('quarkus')
//            library('quarkusJdbcMysql', 'io.quarkus', 'quarkus-jdbc-mysql').versionRef('quarkus')
//            // TODO: need check , Is it necessary to do this?
//            library('quarkusReactivePgClient', 'io.quarkus', 'quarkus-reactive-pg-client').versionRef('quarkus')
//
//            library('redis', 'io.quarkus', 'quarkus-redis-client').versionRef('quarkus')
//
//            // NOTE: tool
//            library('flyway', 'io.quarkus', 'quarkus-flyway').versionRef('quarkus')
//            library('dbHealth', 'io.quarkus', 'quarkus-smallrye-health').versionRef('quarkus')
//            library('graphql', 'io.quarkus', 'quarkus-smallrye-graphql').versionRef('quarkus')
//            library("rest-client-jackson", "io.quarkus", "quarkus-rest-client-jackson").versionRef('quarkus')
//            library("rest-client", "io.quarkus", "quarkus-rest-client-jackson").versionRef('quarkus')
//            library("guava", "com.google.guava", "guava").versionRef('guava')
//            library("opentelemetry", "io.quarkus", "quarkus-opentelemetry").versionRef('quarkus')
//            library('commons', 'org.apache.commons', 'commons-collections4').versionRef('commons')
//            library('micrometer', 'io.quarkus', 'quarkus-micrometer').versionRef('quarkus')
//
//            // NOTE: jooq
//            library('jooq', 'org.jooq', 'jooq').versionRef('jooq')
//            library('jooqMeta', 'org.jooq', 'jooq-meta').versionRef('jooq')
//            library('jooqCodegen', 'org.jooq', 'jooq-codegen').versionRef('jooq')
//
//            // NOTE: search
//            library('es', 'io.quarkus', 'quarkus-elasticsearch-java-client').versionRef('quarkus')
//
//            // NOTE: consul
//            library('consul', 'io.quarkiverse.config', 'quarkus-config-consul').versionRef('quarkus')
//            library('consulStork', 'io.smallrye.stork', 'stork-service-discovery-consul').versionRef('quarkus')
//            library('consulClient', 'io.smallrye.reactive', 'smallrye-mutiny-vertx-consul-client').versionRef('quarkus')
//
//            library('pulsar', 'io.quarkus', 'quarkus-messaging-pulsar').versionRef('quarkus')
//            library('grpc', 'io.quarkus', 'quarkus-grpc').versionRef('quarkus')
//
//            // NOTE: test https://java.testcontainers.org/modules/databases/
//            library('testcontainers', 'org.testcontainers', 'testcontainers').versionRef('testcontainers')
//            library('testcontainersPg', 'org.testcontainers', 'postgresql').versionRef('testcontainers')
//            library('testcontainersConsul', 'org.testcontainers', 'consul').versionRef('testcontainers')
//            library('testcontainersPulsar', 'org.testcontainers', 'pulsar').versionRef('testcontainers')
//            library('testcontainersNeo4j', 'org.testcontainers', 'neo4j').versionRef('testcontainers')
//            library('testcontainersElasticsearch', 'org.testcontainers', 'elasticsearch').versionRef('testcontainers')
//            library('testcontainersMilvus', 'org.testcontainers', 'milvus').versionRef('testcontainers')
//            library('testcontainersJunitJupiter', 'org.testcontainers', 'junit-jupiter').versionRef('testcontainers')
//            library('quarkusJunit5', 'io.quarkus', 'quarkus-junit5').versionRef('quarkus')
//            library('assured', 'io.rest-assured', 'rest-assured').versionRef('quarkus')
//
//            // NOTE: jwt
//            library('jwt', 'io.quarkus', 'quarkus-smallrye-jwt').versionRef('quarkus')
//            library('jwtBuild', 'io.quarkus', 'quarkus-smallrye-jwt-build').versionRef('quarkus')
//
//            // NOTE: SMTP
//            library('smtp', 'io.quarkus', 'quarkus-mailer').versionRef('quarkus')
//            // NOTE: valid
//            library('valid', 'io.quarkus', 'quarkus-hibernate-validator').versionRef('quarkus')
//            // NOTE: share
//            bundle('share', ['quarkusArc'])
//            bundle('web', ['rest'])
////            bundle('share', ['quarkusArc', 'configYaml'])
//            // NOTE: base
//            bundle('base', [
//                    'jackson',
//                    'quarkusSmallryeOpenapi',
//                    'xml',
//                    'routes',
//                    'vertx'
//            ])
//            // NOTE: auth
//            bundle('auth', ['oidc', 'keycloak'])
//            bundle('registerCenterConsul', ['consul', 'consulStork', 'consulClient'])
////            bundle('registerCenterConsul', [ 'consulStork', 'consulClient'])
//            // NOTE: persistence
//            bundle('persistence', [
//                    'quarkusJdbcPostgreSQL',
//                    'quarkusReactivePgClient',
//                    'agroal',
//                    'jooq'
//            ])
//            // NOTE: cache
//            bundle('cache', ['cache', 'redis'])
//            // NOTE: message
//            bundle('mq', ['pulsar'])
//            // NOTE: search
//            bundle('search', ['es'])
//            // NOTE: utils
//            bundle('utils', ['guava'])
//            // NOTE: protocol
//            bundle('protocol', ['grpc', 'websocket'])
//
//            bundle('test', [
////                    'testcontainersJunitJupiter',
////                    'testcontainers',
////                    'testcontainersPg',
////                    'testcontainersPulsar',
////                    'testcontainersConsul',
////                    'testcontainersNeo4j',
////                    'testcontainersElasticsearch',
////                    'testcontainersMilvus',
//'flyway',
//'assured',
//'quarkusJunit5'
//            ])
//        }
//    }
//}
