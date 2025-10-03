# PagoPA eCommerce watchdog deadletter

## Overview

`pagopa-ecommerce-watchdog-deadletter-service` is a Kotlin-based microservice designed to support deadletter monitoring for the eCommerce transactions.
process.
This service leverages Kotlin's native compilation to achieve optimal performance and efficiency.

## Technology Stack

- Kotlin
- Spring Boot (native)

### Environment variables

These are all environment variables needed by the application:

| Variable name                                           | Description                                                                                                                                                | type    | default |
|---------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|---------|
| DOCKER_FILE_NAME                                        | Specifies the Dockerfile to use for the build (e.g., JVM or native)                                                                                        | string  |         |
| ROOT_LOGGING_LEVEL                                      | Sets the root logging level for the application                                                                                                            | string  | INFO    |
| ECOMMERCE_HELPDESK_SERVER_URI                           | URI for the e-commerce helpdesk server                                                                                                                     | string  |         |
| ECOMMERCE_HELPDESK_SERVER_READ_TIMEOUT_MILLIS           | Read timeout in milliseconds for the e-commerce helpdesk server                                                                                            | integer |         |
| ECOMMERCE_HELPDESK_SERVER_CONNECTION_TIMEOUT_MILLIS     | Connection timeout in milliseconds for the e-commerce helpdesk server                                                                                      | integer |         |
| ECOMMERCE_HELPDESK_API_KEY                              | API key for the e-commerce helpdesk service                                                                                                                | string  |         |
| NODO_TECHNICAL_SUPPORT_SERVER_URI                       | URI for the NODO technical support server                                                                                                                  | string  |         |
| NODO_TECHNICAL_SUPPORT_SERVER_READ_TIMEOUT_MILLIS       | Read timeout in milliseconds for the NODO technical support server                                                                                         | integer |         |
| NODO_TECHNICAL_SUPPORT_SERVER_CONNECTION_TIMEOUT_MILLIS | Connection timeout in milliseconds for the NODO technical support server                                                                                   | integer |         |
| NODO_TECHNICAL_SUPPORT_API_KEY                          | API key for the NODO technical support service                                                                                                             | string  |         |
| MONGO_HOST                                              | MongoDB server hostname                                                                                                                                    | string  |         |
| MONGO_PORT                                              | MongoDB server port                                                                                                                                        | integer |         |
| MONGO_USERNAME                                          | Username for MongoDB authentication                                                                                                                        | string  |         |
| MONGO_PASSWORD                                          | Password for MongoDB authentication                                                                                                                        | string  |         |
| MONGO_SSL_ENABLED                                       | Enables or disables SSL for the MongoDB connection                                                                                                         | boolean |         |
| MONGO_DB_NAME                                           | MongoDB database name                                                                                                                                      | string  |         |
| MONGO_MIN_POOL_SIZE                                     | Min amount of connections to be retained into connection pool. See docs *                                                                                  | integer |         |
| MONGO_MAX_POOL_SIZE                                     | Max amount of connections to be retained into connection pool.See docs *                                                                                   | integer |         |
| MONGO_MAX_IDLE_TIMEOUT_MS                               | Max timeout after which an idle connection is killed in milliseconds. See docs *                                                                           | integer |         |
| MONGO_CONNECTION_TIMEOUT_MS                             | Max time to wait for a connection to be opened. See docs *                                                                                                 | integer |         |
| MONGO_SOCKET_TIMEOUT_MS                                 | Max time to wait for a command send or receive before timing out. See docs *                                                                               | integer |         |
| MONGO_SERVER_SELECTION_TIMEOUT_MS                       | Max time to wait for a server to be selected while performing a communication with Mongo in milliseconds. See docs *                                       | integer |         |
| MONGO_WAITING_QUEUE_MS                                  | Max time a thread has to wait for a connection to be available in milliseconds. See docs *                                                                 | integer |         |
| MONGO_HEARTBEAT_FREQUENCY_MS                            | Hearth beat frequency in milliseconds. This is an hello command that is sent periodically on each active connection to perform an health check. See docs * | integer |         |
| MONGO_REPLICA_SET_OPTION                                | Additional replica set options for the MongoDB connection string                                                                                           | string  |         |
| LOGIN_REDIRECT_URL                                      | Url of the login redirect                                                                                                                                  | string  |         |
\* For Mongo connection string options
see [docs](https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/connection/connection-options/#connection-options)

An example configuration of these environment variables is in the `.env.example` file.

It is recommended to create a new .env file by copying the example one, using the following command (make sure you are
in the .env.example folder):

```shell
cp .env.example .env
```

## Working with Windows

If you are developing on Windows, it is recommended the use of WSL2 combined with IntelliJ IDEA.

The IDE should be installed on Windows, with the repository cloned into a folder in WSL2. All the necessary tools will
be installed in the Linux distro of your choice.

You can find more info on how to set up the environment following the link below.

https://www.jetbrains.com/help/idea/how-to-use-wsl-development-environment-in-product.html

After setting up the WSL environment, you can test the application by building it through either Spring Boot or Docker.

## Spring Boot Native

### Requirements

1. You must use GraalVM Java SDK to build native executable locally. It is recommended to use [SDKMAN](https://sdkman.io/install) to provide a better JVM env "switching"
2. This project uses GraalVM gradle plugin which allows to configure a lot of setting for native compilation, like automatic
   toolchain detection https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html

With SDKMAN run the following command to install the required version of Gradle and Java SDK:
```shell
sdk env install
```

#### Compile & Run

To compile microservice to native executable you can use the following gradle task:

```shell
gradle :nativeCompile
```

This will produce an executable inside `build/native/nativeCompile/`

N.B. Compiling into a native executable takes a long time. Locally, it is recommended to launch it normally (in java) in
order to test the service.

Also exist a gradle command to compile and run it directly:

```shell
export $(grep -v '^#' .env.example | xargs)
gradle :nativeRun
```

If you want to run the project locally with Spring Boot, you should initialize the mocks using the following commands (you need yarn installed on the WSL):

```shell
yarn add json-server@0.17.4 --exact
```

The following commands should be used to start the mock servers for local testing

- nodo-technical-support-mock:

```shell
yarn json-server --port 8090 ./docker/nodo-technical-support-mock/nodo-technical-support-server.json --routes ./docker/nodo-technical-support-mock/routes.json --middlewares ./docker/nodo-technical-support-mock/middleware.js
yarn json-server ./ecommerce-helpdesk-mock/helpdesk.json --routes ./ecommerce-helpdesk-mock/routes.json
```

## Docker

The project can be built and run using Docker and docker-compose. You should install Docker Desktop on Windows and go
through its settings to set up the WSL integration.

You can find more info at the following link: https://docs.docker.com/desktop/wsl/

After setting up Docker, you can use the command:

```shell
docker-compose up
```

The docker-compose up command will build the image and start the containers.

#### Tips

The main issue with native image is related to Java Reflection.
GraalVM produces a metadata files containing reflection data. There is also a repository containing the metadata of some
of the most widely used external libraries. You can include this metadata via the gradle plugin

```gradle
graalvmNative {
    metadataRepository {
        enabled.set(true)
        version.set("0.2.6")
    }
}
```

Spring using AOT try automatically to do the best, but you can also find issues.
Here https://docs.spring.io/spring-framework/reference/core/aot.html#aot.hints you can find a lot of tips, like
`@RegisterReflectionForBinding`

#### Dependency lock

This feature use the content of `gradle.lockfile` to check the declared dependencies against the locked one.

If a transitive dependencies have been upgraded the build will fail because of the locked version mismatch.

The following command can be used to upgrade dependency lockfile:

```shell
./gradlew dependencies --write-locks 
```

Running the above command will cause the `gradle.lockfile` to be updated against the current project dependency
configuration

#### Dependency verification

This feature is enabled by adding the gradle `./gradle/verification-metadata.xml` configuration file.

Perform checksum comparison against dependency artifact (jar files, zip, ...) and metadata (pom.xml, gradle module
metadata, ...) used during build
and the ones stored into `verification-metadata.xml` file raising error during build in case of mismatch.

The following command can be used to recalculate dependency checksum:

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

In the above command the `clean`, `spotlessApply` `build` tasks where chosen to be run
in order to discover all transitive dependencies used during build and also the ones used during
spotless apply task used to format source code.

The above command will upgrade the `verification-metadata.xml` adding all the newly discovered dependencies' checksum.
Those checksum should be checked against a trusted source to check for corrispondence with the library author published
checksum.

`/gradlew --write-verification-metadata sha256` command appends all new dependencies to the verification files but does
not remove
entries for unused dependencies.

This can make this file grow every time a dependency is upgraded.

To detect and remove old dependencies make the following steps:

1. Delete, if present, the `gradle/verification-metadata.dryrun.xml`
2. Run the gradle write-verification-metadata in dry-mode (this will generate a verification-metadata-dryrun.xml file
   leaving untouched the original verification file)
3. Compare the verification-metadata file and the verification-metadata.dryrun one checking for differences and removing
   old unused dependencies

The 1-2 steps can be performed with the following commands

```Shell
rm -f ./gradle/verification-metadata.dryrun.xml 
./gradlew --write-verification-metadata sha256 clean spotlessApply build --dry-run
```

The resulting `verification-metadata.xml` modifications must be reviewed carefully checking the generated
dependencies checksum against official websites or other secure sources.

If a dependency is not discovered during the above command execution it will lead to build errors.

You can add those dependencies manually by modifying the `verification-metadata.xml`
file adding the following component:

```xml

<verification-metadata>
    <!-- other configurations... -->
    <components>
        <!-- other components -->
        <component group="GROUP_ID" name="ARTIFACT_ID" version="VERSION">
            <artifact name="artifact-full-name.jar">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
            <artifact name="artifact-pom-file.pom">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
        </component>
    </components>
</verification-metadata>
```

Add those components at the end of the components list and then run the

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

that will reorder the file with the added dependencies checksum in the expected order.

Finally, you can add new dependencies both to gradle.lockfile writing verification metadata running

```shell
 ./gradlew dependencies --write-locks --write-verification-metadata sha256 --no-build-cache --refresh-dependencies
```

For more information read the
following [article](https://docs.gradle.org/8.1/userguide/dependency_verification.html#sec:checksum-verification)

## Contributors 👥

Made with ❤️ by PagoPA S.p.A.

### Maintainers

See `CODEOWNERS` file
