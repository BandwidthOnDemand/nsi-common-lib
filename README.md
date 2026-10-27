# nsi-common-lib
A library for common NSI constants and models.

Build with JDK 21 or newer and Maven 3.9 or newer:

```sh
mvn clean verify
```

The library targets Java 21. `make build` and `make test` use a Maven Docker
image with Eclipse Temurin 21; override `MAVEN_IMAGE` to select another image.

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
Java 21 secure validation stays enabled; module-access flags are unnecessary.
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
