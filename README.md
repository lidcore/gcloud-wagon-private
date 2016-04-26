# gcloud-wagon-private

Deploy and consume artifacts in private S3 repositories. Designed to
be used from [Leiningen](https://github.com/technomancy/leiningen),
but should be usable in other contexts by deploying to repositories at
"gcloud://" URLs.

## Usage

### Leiningen 2.x

Add the plugin and repositories listing to `project.clj`:

```clj
:plugins [[gcloud-wagon-private "1.2.0"]]
```

You can store credentials either in an encrypted file or as
environment variables. For the encrypted file, add this to
`project.clj`:

```clj
:repositories [["private" {:url "gcloud://mybucket/releases/" :creds :gpg}]]
```

And in `~/.lein/credentials.clj.gpg`:

```
{"gcloud://mybucket/releases" {:username "AKIA2489AE28488"
                            :passphrase "98b0b104ca1211e19a6c"}}
```

The username and passphrase here correspond to the AWS Access Key and Secret
Key, respectively.

The map key here can be either a string for an exact match or a regex
checked against the repository URL if you have the same credentials
for multiple repositories.

To use the environment for credentials, include
`:username :env :passphrase :env` instead of `:creds :gpg` and export
`LEIN_USERNAME` and `LEIN_PASSPHRASE` environment variables.

See `lein help deploying` for details on storing credentials.

If you are running Leiningen in an environment where you don't control
the user such as Heroku or Jenkins, you can include credentials in the
`:repositories` entry. However, you should avoid committing them to
your project, so you should take them from the environment using
`System/getenv`:

```clj
(defproject my-project "1.0.0"
  :plugins [[gcloud-wagon-private "1.2.0"]]
  :repositories {"releases" {:url "gcloud://mybucket/releases/"
                             :username :env/aws_access_key ;; gets environment variable AWS_ACCESS_KEY
                             :passphrase :env/aws_secret_key}}) ;; gets environment variable AWS_SECRET_KEY 
```

### Maven

#### pom.xml

```xml
     <build>
        <extensions>
            <extension>
                <groupId>gcloud-wagon-private</groupId>
                <artifactId>gcloud-wagon-private</artifactId>
                <version>1.2.0</version>
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
            <username>${env.AWS_ACCESS_KEY}</username>
            <passphrase>${env.AWS_SECRET_KEY}</passphrase>
        </server>
    </servers>
</settings>

```

## License

Copyright © 2016 Lidcore

Based on [s3-wagon-private](https://github.com/technomancy/s3-wagon-private)

Distributed under the Apache Public License version 2.0.
