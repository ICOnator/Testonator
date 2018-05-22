# testrpcj

This library provides an Ethereum test blockchain for testing purposes.

## Add the dependency

Maven:

```
<dependency>
    <groupId>io.iconator</groupId>
    <artifactId>testrpcj</artifactId>
    <version>1.0.1</version>
</dependency>
```

Gradle:

```
compile 'io.iconator:testrpcj:1.0.1'
```

## How to deploy to Maven Central

First, make sure that you have a `gradle.properties` with the following variables set:

```
nexusUsername=<USERNAME>
nexusPassword=<PASSWORD>

signing.keyId=<KEY_ID>
signing.password=<KEY_PASSWORD>
signing.secretKeyRingFile=<KEY_RING_FILE>
```

Second, clean and build the project. Make sure that you increase the project version as well. :-)

```
$ sh gradlew clean build
```

Then, upload the whole archive:

```
$ sh gradlew uploadArchives
```

If everything is successfully uploaded, close the staged project and release the repository:

```
$ sh gradlew closeAndReleaseRepository
```

If something gets wrong, [this tutorial](http://www.albertgao.xyz/2018/01/18/how-to-publish-artifact-to-maven-central-via-gradle/)
is a nice source of knowledge to debug and fix it.