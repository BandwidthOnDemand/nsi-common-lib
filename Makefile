# These files need to be under the sense-rm build context so they
# are available under the docker mount.
#KEYFILE="certificates/host.key"
#CERTFILE="certificates/host.cer"
#CAFILE="certificates/host.ca"

MAVEN_IMAGE ?= maven:3.10.0-eclipse-temurin-17

all:	build 

.PHONY: build test clean
build:
	docker run -it --rm --name nsi-common-lib-build \
		-v "$(PWD)":/usr/src/mymaven \
		-v "$(HOME)/.m2":/root/.m2  \
		-w /usr/src/mymaven \
		$(MAVEN_IMAGE) mvn clean install -DskipTests=true

test:
	docker run -it --rm --name nsi-common-lib-test \
		-v "$(PWD)":/usr/src/mymaven \
		-v "$(HOME)/.m2":/root/.m2  \
		-w /usr/src/mymaven \
		-p 8801:8801 \
		-p 8802:8802 \
		$(MAVEN_IMAGE) mvn clean install

# -Dtest=AgoleManifestReaderTest#loadMasterList

clean:
	docker run -it --rm --name nsi-common-lib-clean \
                -v "$(PWD)":/usr/src/mymaven \
                -v "$(HOME)/.m2":/root/.m2  \
                -w /usr/src/mymaven \
                $(MAVEN_IMAGE) mvn clean

docker:

