![Travis](https://img.shields.io/travis/ICOnator/Testonator.svg) ![Maven metadata URI](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/io/iconator/testonator/maven-metadata.xml.svg)

# Testonator

This library provides a JSON-RPC wrapper to a standalone Ethereum blockchain, used for testing purposes. It uses Solidity 0.4.25. It also provides an wrapper for web3j, where contracts do not have to be generated into Java classes. If your contract looks like this:

```
pragma solidity ^0.4.25;
               
contract Example2 {
    uint256 public counter = 15;
    function setMyCounter(uint256 _counter) public returns (uint256) {
        uint256 tmp = counter;
        counter = _counter;
        return tmp;
    }
}
```

You can use this library to call the function with a FunctionBuilder:

```

Credentials cred = fromECPrivateKey("1b86...");
String contractAddress = "0xa5...";
testBlockchain.call(cred, contractAddress, 
    new FunctionBuilder("setMyCounter")
        .addInput("uint256", new BigInteger("23"))
        .outputs("uint256"));
```


## Installation: add the dependency

Maven:

```
<dependency>
    <groupId>io.iconator</groupId>
    <artifactId>testonator</artifactId>
    <version>1.0.32</version>
</dependency>
```

Gradle:

```
compile 'io.iconator:testonator:1.0.32'
```

## Example of JSON-RPC request

You can instantiate the `TestBlockchain` class, and then run the following:

```
$ curl --silent -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_getBlockByNumber","params":["latest", true],"id":1}' http://localhost:8545/rpc
```

The default port is `8545`.

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
