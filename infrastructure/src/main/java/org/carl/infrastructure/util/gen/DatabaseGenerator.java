package org.carl.infrastructure.util.gen;

import io.quarkus.arc.profile.IfBuildProfile;
import org.jooq.codegen.JavaGenerator;
import org.jooq.codegen.JavaWriter;
import org.jooq.meta.TableDefinition;

@IfBuildProfile(anyOf = {"test", "dev"})
public class DatabaseGenerator extends JavaGenerator {

    @Override
    protected void generatePojoClassFooter(TableDefinition table, JavaWriter out) {
        super.generatePojoClassFooter(table, out);
    }
}
