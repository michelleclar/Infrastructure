package org.carl.infrastructure.util;

import io.quarkus.arc.profile.IfBuildProfile;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;

@IfBuildProfile(anyOf = {"test", "dev"})
public class JooqCodeGenerator {

    String jdbcUrl =
            ConfigProvider.getConfig().getValue("quarkus.datasource.jdbc.url", String.class);

    String username =
            ConfigProvider.getConfig().getValue("quarkus.datasource.username", String.class);

    String password =
            ConfigProvider.getConfig().getValue("quarkus.datasource.password", String.class);

    public void codeGenByContainers() throws Exception {

        // Generate JOOQ code programmatically
        Configuration configuration =
                new Configuration()
                        .withJdbc(
                                new Jdbc()
                                        .withDriver("org.postgresql.Driver")
                                        .withUrl(jdbcUrl)
                                        .withUser(username)
                                        .withPassword(password))
                        .withGenerator(
                                new Generator()
                                        .withDatabase(
                                                new Database()
                                                        .withName("org.jooq.meta.postgres.PostgresDatabase")
                                                        .withInputSchema("public")
                                                        .withOutputSchemaToDefault(true) // don't generator schema
                                                        .withIncludes(".*")
                                                        .withExcludes(""))
                                        .withGenerate(
                                                new Generate()
                                                        .withPojos(true)
                                                        .withDeprecated(false)
                                                        .withRecords(true)
                                                        .withImmutablePojos(false)
                                                        .withImmutableInterfaces(true)
                                                        .withInterfaces(true)
                                                        .withFluentSetters(true)
                                                        .withDaos(true))
                                        .withTarget(
                                                new Target()
                                                        .withPackageName("org.carl.generated")
                                                        .withDirectory("../application/src/main/generated")));

        GenerationTool.generate(configuration);
    }
}