package org.carl.infrastructure.workflow.spi;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Best-effort static lint that scans a {@link NodeHandler} class's bytecode constant pool for
 * references to types and methods that strongly suggest non-deterministic behavior (wall-clock
 * time, IO, randomness, blocking).
 *
 * <p><strong>This is a lint, not a sandbox.</strong> It catches obvious cases where forbidden JDK
 * symbols appear in the constant pool of the handler class itself. It does NOT analyze:
 *
 * <ul>
 *   <li>helper classes the handler delegates to (reflection or interface call chains);
 *   <li>dependency-injected services whose types are interfaces, not JDK classes;
 *   <li>lambdas/method references defined in other classes;
 *   <li>native methods or JNI.
 * </ul>
 *
 * <p>It also has false positives: a handler that contains the string literal {@code
 * "java/util/Random"} or whose constant pool happens to mention a forbidden type for benign reasons
 * (e.g. a {@code Class} reference used only in a comment-equivalent constant) will be flagged.
 * {@code DeterminismGuard} is intended to complement — not replace — code review.
 *
 * <p><strong>Activation.</strong> {@link NodeHandlerRegistry#register(NodeHandler)} calls {@link
 * #assertPure(Class)} for every user-registered handler — no opt-in required. Built-in handlers
 * bypass this check via {@link NodeHandlerRegistry#registerBuiltIn(NodeHandler)}, which is the
 * trusted path reserved for handlers shipped with the infrastructure module.
 *
 * <p>Real safety still depends on developer discipline. Handlers that need IO/time/randomness
 * should delegate via {@code RuntimeIntents.ACTIVITY} so that side-effects happen outside the
 * deterministic replay boundary.
 */
public final class DeterminismGuard {

    /**
     * Forbidden JDK types whose presence in a handler's constant pool is highly suspicious.
     *
     * <p>The set targets sources of non-determinism relevant to workflow replay: wall-clock time
     * ({@code Date}, {@code Clock}), randomness ({@code UUID}, {@code Random},
     * {@code SecureRandom}), network I/O ({@code HttpClient}), and blocking filesystem/database
     * access ({@code File*}, {@code Connection}, {@code DriverManager}). All side-effectful
     * operations must instead be delegated through {@code RuntimeIntents.ACTIVITY}.
     *
     * <p>{@code java.lang.System} is deliberately absent: a type-level hit would also flag benign
     * uses such as {@code System.arraycopy} or identity hash codes. Its unsafe members
     * ({@code currentTimeMillis}/{@code nanoTime}) are covered by {@link #FORBIDDEN_METHODS}.
     */
    public static final Set<String> FORBIDDEN_TYPES =
            Set.of(
                    "java.util.Date",
                    "java.time.Clock",
                    "java.util.UUID",
                    "java.util.Random",
                    "java.security.SecureRandom",
                    "java.net.http.HttpClient",
                    "java.io.File",
                    "java.io.FileInputStream",
                    "java.io.FileOutputStream",
                    "java.sql.Connection",
                    "java.sql.DriverManager");

    /**
     * Forbidden methods in the form {@code fully.qualified.Type#method}.
     *
     * <p>Checked at a finer granularity than {@link #FORBIDDEN_TYPES} for cases where the type
     * itself is not universally suspicious (e.g. {@code java.lang.System} is also used for
     * identity hash codes, which are benign) but specific methods on it are not safe in a
     * deterministic replay. {@code Thread#sleep} is listed because spinning/blocking inside a
     * handler breaks Temporal's event-loop threading model.
     */
    public static final Set<String> FORBIDDEN_METHODS =
            Set.of(
                    "java.time.Instant#now",
                    "java.time.LocalDateTime#now",
                    "java.time.LocalDate#now",
                    "java.lang.System#currentTimeMillis",
                    "java.lang.System#nanoTime",
                    "java.lang.Thread#sleep",
                    "java.lang.Math#random");

    private DeterminismGuard() {
        throw new AssertionError("no instances");
    }

    /**
     * Scan the bytecode of {@code handlerClass} for references to forbidden types and methods.
     *
     * @return a (possibly empty) human-readable list of violations. An empty list means the lint
     *     found nothing suspicious — it does NOT prove determinism.
     */
    public static List<String> staticScan(Class<? extends NodeHandler<?, ?, ?>> handlerClass) {
        List<String> violations = new ArrayList<>();
        byte[] bytes;
        try {
            bytes = readClassFile(handlerClass);
        } catch (IOException e) {
            violations.add("could not read class file for static scan: " + e.getMessage());
            return violations;
        }
        ConstantPool pool;
        try {
            pool = ConstantPool.parse(bytes);
        } catch (IOException e) {
            violations.add("could not parse class file: " + e.getMessage());
            return violations;
        }

        Set<String> referencedClasses = pool.referencedClassNames();
        Set<String> referencedMethods = pool.referencedMethods();

        for (String forbidden : FORBIDDEN_TYPES) {
            // The constant pool stores class names in internal (slash-separated) form.
            String internal = forbidden.replace('.', '/');
            if (referencedClasses.contains(internal)) {
                violations.add("references forbidden type " + forbidden);
            }
        }
        for (String forbidden : FORBIDDEN_METHODS) {
            // Reconstruct the "owner#methodName" key that referencedMethods() emits, converting
            // the dot-separated class name to internal slash form to match the bytecode.
            int hash = forbidden.indexOf('#');
            String owner = forbidden.substring(0, hash).replace('.', '/');
            String name = forbidden.substring(hash + 1);
            String key = owner + "#" + name;
            if (referencedMethods.contains(key)) {
                violations.add("calls forbidden method " + forbidden);
            }
        }
        return violations;
    }

    /**
     * Throws {@link IllegalStateException} if {@link #staticScan(Class)} returns a non-empty list.
     *
     * @throws IllegalStateException with the handler class name and a bullet list of violations.
     */
    public static void assertPure(Class<? extends NodeHandler<?, ?, ?>> handlerClass) {
        List<String> violations = staticScan(handlerClass);
        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "NodeHandler "
                            + handlerClass.getName()
                            + " violates determinism contract:\n  - "
                            + String.join("\n  - ", violations));
        }
    }

    private static byte[] readClassFile(Class<?> cls) throws IOException {
        String resource = "/" + cls.getName().replace('.', '/') + ".class";
        try (InputStream in = cls.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("class resource not found: " + resource);
            }
            return in.readAllBytes();
        }
    }

    /**
     * Minimal class-file constant pool parser. We only need {@code CONSTANT_Class} (7), {@code
     * CONSTANT_Utf8} (1), {@code CONSTANT_Methodref} (10), {@code CONSTANT_InterfaceMethodref}
     * (11), and {@code CONSTANT_NameAndType} (12). All other tags are skipped by length.
     */
    private static final class ConstantPool {

        private static final int CONSTANT_Utf8 = 1;
        private static final int CONSTANT_Integer = 3;
        private static final int CONSTANT_Float = 4;
        private static final int CONSTANT_Long = 5;
        private static final int CONSTANT_Double = 6;
        private static final int CONSTANT_Class = 7;
        private static final int CONSTANT_String = 8;
        private static final int CONSTANT_Fieldref = 9;
        private static final int CONSTANT_Methodref = 10;
        private static final int CONSTANT_InterfaceMethodref = 11;
        private static final int CONSTANT_NameAndType = 12;
        private static final int CONSTANT_MethodHandle = 15;
        private static final int CONSTANT_MethodType = 16;
        private static final int CONSTANT_Dynamic = 17;
        private static final int CONSTANT_InvokeDynamic = 18;
        private static final int CONSTANT_Module = 19;
        private static final int CONSTANT_Package = 20;

        // index -> resolved info
        private final String[] utf8;
        private final int[] classNameIndex; // for CONSTANT_Class: index into utf8
        // for method refs: pair (classIndex, nameAndTypeIndex)
        private final int[] methodClassIndex;
        private final int[] methodNameAndType;
        // for NameAndType: pair (nameIndex, descriptorIndex) — only nameIndex needed here
        private final int[] nameAndTypeNameIndex;

        private ConstantPool(int size) {
            this.utf8 = new String[size];
            this.classNameIndex = new int[size];
            this.methodClassIndex = new int[size];
            this.methodNameAndType = new int[size];
            this.nameAndTypeNameIndex = new int[size];
        }

        static ConstantPool parse(byte[] bytes) throws IOException {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                int magic = in.readInt();
                if (magic != 0xCAFEBABE) {
                    throw new IOException("not a class file (bad magic)");
                }
                in.readUnsignedShort(); // minor
                in.readUnsignedShort(); // major
                // cpCount is one greater than the actual number of entries; index 0 is reserved.
                int cpCount = in.readUnsignedShort();
                ConstantPool pool = new ConstantPool(cpCount);
                int i = 1;
                while (i < cpCount) {
                    int tag = in.readUnsignedByte();
                    switch (tag) {
                        case CONSTANT_Utf8 -> {
                            int len = in.readUnsignedShort();
                            byte[] data = new byte[len];
                            in.readFully(data);
                            // Modified UTF-8 — for ASCII identifiers (the only thing we care about
                            // here) plain UTF-8 decoding matches.
                            pool.utf8[i] =
                                    new String(data, java.nio.charset.StandardCharsets.UTF_8);
                        }
                        case CONSTANT_Integer, CONSTANT_Float -> in.readInt();
                        case CONSTANT_Long, CONSTANT_Double -> {
                            in.readLong();
                            // Long/Double constants occupy two consecutive constant-pool slots.
                            // The JVM spec §4.4.5 explicitly requires incrementing the index twice.
                            i++;
                        }
                        case CONSTANT_Class -> pool.classNameIndex[i] = in.readUnsignedShort();
                        case CONSTANT_String -> in.readUnsignedShort();
                        case CONSTANT_Fieldref -> {
                            in.readUnsignedShort();
                            in.readUnsignedShort();
                        }
                        case CONSTANT_Methodref, CONSTANT_InterfaceMethodref -> {
                            pool.methodClassIndex[i] = in.readUnsignedShort();
                            pool.methodNameAndType[i] = in.readUnsignedShort();
                        }
                        case CONSTANT_NameAndType -> {
                            pool.nameAndTypeNameIndex[i] = in.readUnsignedShort();
                            in.readUnsignedShort(); // descriptor index
                        }
                        case CONSTANT_MethodHandle -> {
                            in.readUnsignedByte();
                            in.readUnsignedShort();
                        }
                        case CONSTANT_MethodType, CONSTANT_Module, CONSTANT_Package ->
                                in.readUnsignedShort();
                        case CONSTANT_Dynamic, CONSTANT_InvokeDynamic -> {
                            in.readUnsignedShort();
                            in.readUnsignedShort();
                        }
                        default ->
                                throw new IOException(
                                        "unknown constant pool tag " + tag + " at index " + i);
                    }
                    i++;
                }
                return pool;
            }
        }

        Set<String> referencedClassNames() {
            Set<String> out = new LinkedHashSet<>();
            for (int i = 1; i < classNameIndex.length; i++) {
                int nameIdx = classNameIndex[i];
                if (nameIdx > 0 && nameIdx < utf8.length && utf8[nameIdx] != null) {
                    out.add(utf8[nameIdx]);
                }
            }
            return out;
        }

        Set<String> referencedMethods() {
            Set<String> out = new LinkedHashSet<>();
            for (int i = 1; i < methodClassIndex.length; i++) {
                int classRef = methodClassIndex[i];
                int natRef = methodNameAndType[i];
                // Skip slots that were not populated as Methodref/InterfaceMethodref entries.
                if (classRef <= 0 || natRef <= 0) continue;
                if (classRef >= classNameIndex.length || natRef >= nameAndTypeNameIndex.length) {
                    continue;
                }
                // Two-level indirection: Methodref → Class → Utf8 for the owner name,
                // and Methodref → NameAndType → Utf8 for the method name.
                int ownerNameIdx = classNameIndex[classRef];
                int methodNameIdx = nameAndTypeNameIndex[natRef];
                if (ownerNameIdx <= 0 || methodNameIdx <= 0) continue;
                if (ownerNameIdx >= utf8.length || methodNameIdx >= utf8.length) continue;
                String owner = utf8[ownerNameIdx];
                String name = utf8[methodNameIdx];
                if (owner != null && name != null) {
                    out.add(owner + "#" + name);
                }
            }
            return out;
        }
    }
}
