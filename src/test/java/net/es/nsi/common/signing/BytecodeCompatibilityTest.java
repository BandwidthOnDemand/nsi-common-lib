package net.es.nsi.common.signing;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Guards bytecode checks against preview classes and incorrect multi-release selection. */
class BytecodeCompatibilityTest {
    /** Isolated synthetic archives. */
    @TempDir Path directory;

    /**
     * Requires exact Java 17 library bytecode and rejects preview headers.
     * @throws Exception if fixture creation fails
     */
    @Test
    void libraryRejectsNewerOlderAndPreviewClasses() throws Exception {
        BytecodeCompatibility.library(archive("valid", false, 61, 0, 0));
        assertThrows(IOException.class, () -> BytecodeCompatibility.library(archive("newer", false, 62, 0, 0)));
        assertThrows(IOException.class, () -> BytecodeCompatibility.library(archive("older", false, 60, 0, 0)));
        assertThrows(IOException.class, () -> BytecodeCompatibility.library(archive("preview", false, 61, 65535, 0)));
    }

    /**
     * Allows newer MR entries only on runtimes selecting them, and honors the MR manifest.
     * @throws Exception if fixture creation fails
     */
    @Test
    void dependencyUsesEffectiveRuntimeEntries() throws Exception {
        Path valid = archive("mr", true, 61, 0, 65);
        BytecodeCompatibility.dependency(valid, 17);
        BytecodeCompatibility.dependency(valid, 21);
        BytecodeCompatibility.dependency(valid, 27);
        Path invalid = archive("invalid-mr", true, 61, 0, 66);
        BytecodeCompatibility.dependency(invalid, 17);
        assertThrows(IOException.class, () -> BytecodeCompatibility.dependency(invalid, 21));
        BytecodeCompatibility.dependency(archive("no-mr-manifest", false, 61, 0, 66), 21);
    }

    /**
     * Writes class headers sufficient for checks that intentionally never load classes.
     * @param name unique fixture archive basename
     * @param multiRelease whether to enable runtime version selection
     * @param major base class major version
     * @param minor base class minor version
     * @param versionedMajor optional Java 21 MR class major version; zero omits it
     * @return created archive
     * @throws IOException if writing fails
     */
    private Path archive(String name, boolean multiRelease, int major, int minor, int versionedMajor)
            throws IOException {
        Path path = directory.resolve(name + ".jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (multiRelease) { manifest.getMainAttributes().put(Attributes.Name.MULTI_RELEASE, "true"); }
        try (var output = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            entry(output, "example/Consumer.class", major, minor);
            if (versionedMajor != 0) {
                entry(output, "META-INF/versions/21/example/Consumer.class", versionedMajor, 0);
            }
        }
        return path;
    }

    /**
     * Adds a synthetic class header without closing the caller-owned archive.
     * @param output archive owned by the caller
     * @param name entry path
     * @param major class-file major version
     * @param minor class-file minor version
     * @throws IOException if writing fails
     */
    private static void entry(JarOutputStream output, String name, int major, int minor) throws IOException {
        output.putNextEntry(new JarEntry(name));
        DataOutputStream data = new DataOutputStream(output);
        data.writeInt(0xCAFEBABE);
        data.writeShort(minor);
        data.writeShort(major);
        output.closeEntry();
    }
}
