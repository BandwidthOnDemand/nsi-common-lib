package net.es.nsi.common.signing;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/** Checks packaged bytecode independently of the JDK running the tests. */
final class BytecodeCompatibility {
    /** Prevents construction of this test utility. */
    private BytecodeCompatibility() { }

    /**
     * Requires all packaged library classes, including generated models, to target Java 17.
     * @param path library JAR to inspect
     * @throws IOException if the archive or a class header is invalid or incompatible
     */
    static void library(Path path) throws IOException {
        try (JarFile jar = new JarFile(path.toFile())) {
            var entries = jar.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (var input = new DataInputStream(jar.getInputStream(entry))) {
                        header(input, 61, true, path + "!" + entry.getName());
                    }
                    count++;
                }
            }
            if (count == 0) {
                throw new IOException("No library classes in " + path);
            }
        }
    }

    /**
     * Checks classes actually selected by the JVM from a dependency, honoring its MR manifest.
     * @param path production dependency JAR
     * @param javaVersion Java runtime major version
     * @throws IOException if a selected class cannot run on that Java version
     */
    static void dependency(Path path, int javaVersion) throws IOException {
        try (JarFile jar = new JarFile(path.toFile(), true, ZipFile.OPEN_READ,
                Runtime.Version.parse(Integer.toString(javaVersion)))) {
            for (var entry : jar.versionedStream().toList()) {
                if (entry.getName().endsWith(".class") && !entry.getName().startsWith("META-INF/")) {
                    try (var input = new DataInputStream(jar.getInputStream(entry))) {
                        header(input, javaVersion + 44, false,
                                path + "!" + entry.getRealName() + " on Java " + javaVersion);
                    }
                }
            }
        }
    }

    /**
     * Validates a class header without loading or executing the class.
     * @param input class bytes; owned and closed by the caller
     * @param maximum maximum accepted class-file major version
     * @param exact whether the major version must equal the maximum
     * @param location diagnostic archive and entry name
     * @throws IOException if magic, preview status, or version is incompatible
     */
    private static void header(DataInputStream input, int maximum, boolean exact, String location)
            throws IOException {
        if (input.readInt() != 0xCAFEBABE) {
            throw new IOException("Invalid class magic: " + location);
        }
        int minor = input.readUnsignedShort();
        int major = input.readUnsignedShort();
        if (minor == 65535 || major > maximum || (exact && major != maximum)) {
            throw new IOException("Incompatible class version " + major + "." + minor + ": " + location);
        }
    }
}
