# nsi-common-lib
A library for common NSI constants and models.

Build with JDK 17, 21, 25, or 27 and Maven 3.9 or newer:

```sh
mvn clean verify
```

The library targets Java 17 bytecode. Build and runtime support is limited to
Java 17, 21, 25, and 27. Other Java versions are outside the supported configuration.
`make build` and `make test` default to `maven:3.10.0-eclipse-temurin-17`;
override `MAVEN_IMAGE` to select another image for a supported Java version.
Support for a Java release does not extend its vendor's maintenance or
security-update period.

The Java compatibility workflow performs fresh builds on all four supported JDKs on
Linux x64 with Temurin and Maven 3.10.0. It also runs a standalone consumer on
each supported runtime against the same Java 17-built JAR, using only production
dependencies. Other operating systems and JDK distributions require independent
validation. The `Java compatibility` aggregate CI check fails if any matrix entry
fails; configure it as a required branch-protection check in the hosting service.
No remote repository settings are changed by this workflow.

`clean verify` checks packaged class-file versions and runs the consumer against
the locally built JAR. To test a canonical artifact built elsewhere, keep it
outside `target` (which `clean` removes) and use:

```sh
mvn --batch-mode --no-transfer-progress clean verify -Dcompatibility.jar=/absolute/path/common-lib.jar
```

The consumer covers JAXB/Unicode, RSA/DSA signing and tampering rejection,
codecs, labels, and URL behavior. Artifact checks require Java 17 class files
without preview features and inspect runtime-selected dependency classes for
each supported Java version. Handwritten Javadoc is validated separately
on JDK 17 and JDK 27 with `-Xdoclint:all,-missing -Werror`.

Dependencies and build plugins use stable releases with fixed versions, managed
by the Spring Boot parent and the properties in `pom.xml`.

The XML bindings and mail utilities now use Jakarta XML Binding 4 and Jakarta
Mail 2. Consumers must replace `javax.xml.bind.*` and `javax.mail.*` imports with
`jakarta.xml.bind.*` and `jakarta.mail.*` and regenerate any interacting JAXB
models with Jakarta annotations. Java SE XML APIs such as `javax.xml.namespace`
and `javax.xml.datatype` retain their existing packages. A JAXB runtime and the
Angus Mail implementation are included as runtime dependencies.

XML signing uses RSA-SHA256 or DSA-SHA256 with SHA-512 reference digests.
SHA-1 signatures are rejected, and unsupported signing key types fail explicitly.
XML signature secure validation stays enabled; module-access flags are unnecessary.
The `jsr105Provider` system property can select a registered provider by name or
class name, or a public third-party provider class with a public no-argument
constructor. Without an override, signing uses standard JCA provider discovery.

`generateExternalSignature(document, alias)` returns a separate signature document
and preserves the source DOM. Its reference URI is
`urn:net:es:nsi:common:document`; `Validate.validateExternal(document, signature)`
resolves that reference to the supplied document locally. The document is
canonicalized using Canonical XML 1.0 with comments before its digest is computed.
Verification rejects other reference targets and transforms and performs no
external URI fetches. Preserve XML text whitespace when saving the document,
since canonicalization preserves that whitespace.

Signature validation checks integrity using the certificate embedded in `KeyInfo`.
Applications remain responsible for deciding whether that certificate is trusted.

`mvn clean verify` also produces JaCoCo HTML, XML, and CSV coverage reports in
`target/site/jacoco/` and enforces 85% line coverage and 75% branch coverage across
handwritten code. Generated JAXB model packages are excluded; parser wrappers
remain included. The coverage properties in `pom.xml` define these minimums.
After dependencies are cached, use `mvn -o clean verify` for an offline run.

Tests use temporary files and keystores, restore global settings, and require no
external services. Seeded label-set checks supplement explicit boundary and
error cases. XML string inputs are treated as decoded characters, while byte
streams honor their XML encoding declaration. DOM, JAXB parser, and XML utility
entry points reject DTDs and external entities and leave caller-owned input
streams open. Methods opening a file manage that file's stream themselves.

The regression suite also enforces these behavior corrections: numeric labels
compare by type and normalized value; mixed label groups round-trip; STPs own
their label sets; outbound topology ports are selected independently of inbound
ports; gzip payloads are decompressed once; and missing signing aliases and
ambiguous signatures produce explicit failures. Invalid or excessive numeric
ranges are rejected before expansion (a single range supports at most the MPLS
label-space size of 1,048,576 values).
