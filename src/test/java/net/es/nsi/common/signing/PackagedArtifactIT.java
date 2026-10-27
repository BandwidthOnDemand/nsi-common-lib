package net.es.nsi.common.signing;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Runs a real consumer against a packaged artifact and production dependencies only. */
class PackagedArtifactIT {
    /** Temporary consumer classes, credentials, stores, and captured process output. */
    @TempDir Path directory;

    /**
     * Checks local and selected artifacts and runtime-selected dependency bytecode across the window.
     * @throws Exception if packaging or compatibility checks fail
     */
    @Test
    void packagedBytecodeSupportsTheWholeWindow() throws Exception {
        BytecodeCompatibility.library(Path.of(System.getProperty("compatibility.localJar")));
        BytecodeCompatibility.library(selectedJar());
        for (Path dependency : dependencies()) {
            for (int javaVersion = 17; javaVersion <= 27; javaVersion++) {
                BytecodeCompatibility.dependency(dependency, javaVersion);
            }
        }
    }

    /**
     * Compiles a Java 17 consumer and runs it with the selected artifact on the current JDK.
     * @throws Exception if compilation, fixture generation, or consumer verification fails
     */
    @Test
    void standaloneConsumerUsesPackagedArtifact() throws Exception {
        Path jar = selectedJar();
        String classpath = jar + File.pathSeparator + String.join(File.pathSeparator,
                dependencies().stream().map(Path::toString).toList());
        Path source = directory.resolve("CompatibilityConsumer.java");
        try (var input = getClass().getResourceAsStream("/compatibility/CompatibilityConsumer.java")) {
            assertNotNull(input);
            Files.copy(input, source);
        }
        run("compile", List.of(tool("javac"), "--release", "17", "-encoding", "UTF-8",
                "-proc:none", "-classpath", classpath, "-d", directory.toString(), source.toString()));
        for (String algorithm : List.of("RSA", "DSA")) {
            SigningTestSupport.createKeystore(directory.resolve(algorithm + ".p12"), algorithm);
        }
        // Keep the generated credential out of command-line diagnostics and the consumer source.
        Path credential = directory.resolve("credential");
        Files.writeString(credential, SigningTestSupport.PASSWORD, StandardCharsets.UTF_8);
        run("consumer", List.of(tool("java"), "-classpath", directory + File.pathSeparator + classpath,
                "CompatibilityConsumer", jar.toString(), directory.toString(), credential.toString()));
    }

    /**
     * Resolves the selected artifact without allowing a directory of compiled classes.
     * @return existing normalized JAR path
     * @throws Exception if the configured artifact is absent
     */
    private static Path selectedJar() throws Exception {
        Path jar = Path.of(System.getProperty("compatibility.jar")).toRealPath();
        assertTrue(Files.isRegularFile(jar), "Expected a packaged JAR: " + jar);
        return jar;
    }

    /**
     * Reads Maven's runtime-scope classpath, excluding test dependencies and project classes.
     * @return normalized production dependency JAR paths
     * @throws Exception if classpath generation or resolution failed
     */
    private static List<Path> dependencies() throws Exception {
        String classpath = Files.readString(Path.of(System.getProperty("compatibility.classpathFile"))).trim();
        assertFalse(classpath.isEmpty(), "Production dependency classpath is empty");
        var paths = new ArrayList<Path>();
        for (String entry : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            Path path = Path.of(entry).toRealPath();
            assertTrue(Files.isRegularFile(path), "Dependency must be a JAR: " + path);
            paths.add(path);
        }
        return paths;
    }

    /**
     * Finds a tool in the JDK running Failsafe, including on Windows.
     * @param name tool basename
     * @return tool executable path
     */
    private static String tool(String name) {
        String executable = System.getProperty("os.name").startsWith("Windows") ? name + ".exe" : name;
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    /**
     * Runs a bounded child process and retains its output for assertion diagnostics.
     * @param name log basename
     * @param command process argument vector
     * @throws Exception if execution fails or the waiting thread is interrupted
     */
    private void run(String name, List<String> command) throws Exception {
        Path log = directory.resolve(name + ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(60, TimeUnit.SECONDS),
                    () -> name + " timed out; diagnostics: " + log);
            assertEquals(0, process.exitValue(), () -> {
                try { return Files.readString(log); }
                catch (Exception error) { return "Cannot read " + log + ": " + error; }
            });
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Child process did not terminate: " + name);
            }
        }
    }
}
