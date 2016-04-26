# gcloud-wagon-private

Deploy and consume artifacts in private Google Cloud Storage repositories. Designed to
be used from [Leiningen](https://github.com/technomancy/leiningen),
but should be usable in other contexts by deploying to repositories at
"gcloud://" URLs.

## Usage

### Leiningen 2.x

Add the plugin and repositories listing to `project.clj`:

```clj
:plugins [[gcloud-wagon-private "0.1.0"]]
```

Credentials are fetched using the (Google Application Default Credentials)[https://developers.google.com/identity/protocols/application-default-credentials]
however, Leiningen still requires to pass username/password so you can add the following to your
`project.clj`:

```clj
:repositories [["private" {:url "gcloud://mybucket/releases/" :username "" :password ""}]]
```

### Maven

#### pom.xml

```xml
     <build>
        <extensions>
            <extension>
                <groupId>gcloud-wagon-private</groupId>
                <artifactId>gcloud-wagon-private</artifactId>
                <version>0.1.0</version>
            </extension>
        </extensions>
    </build>

    <!-- to publish to a private bucket -->

     <distributionManagement>
                <repository>
                    <id>someId</id>
                    <name>Some Name</name>
                    <url>gcloud://some-bucket/release</url>
                </repository>
                <snapshotRepository>
                    <id>someSnapshotId</id>
                    <name>Some Snapshot Name</name>
                    <url>gcloud://some-bucket/snapshot</url>
                </snapshotRepository>
     </distributionManagement>

     <!-- to consume artifacts from a private bucket -->

     <repositories>
        <repository>
            <id>someId</id>
            <name>Some Name</name>
            <url>gcloud://some-bucket/release</url>
        </repository>
    </repositories>


```

#### settings.xml



```xml


<settings>
    <servers>
        <server>
            <!-- you can actually put the key and secret in here, I like to get them from the env -->
            <id>someId</id>
            <username></username>
            <passphrase></passphrase>
        </server>
    </servers>
</settings>

```

## License

Copyright © 2016 Lidcore

Based on [s3-wagon-private](https://github.com/technomancy/s3-wagon-private)

Distributed under the Apache Public License version 2.0.
