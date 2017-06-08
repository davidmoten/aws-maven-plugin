aws-maven-plugin
-----------------

<a href="https://travis-ci.org/davidmoten/aws-maven-plugin"><img src="https://travis-ci.org/davidmoten/aws-maven-plugin.svg"/></a><br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/aws-maven-plugin/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/aws-maven-plugin)<br/>
<!--[![Dependency Status](https://gemnasium.com/com.github.davidmoten/aws-maven-plugin.svg)](https://gemnasium.com/com.github.davidmoten/aws-maven-plugin)-->

* Deploy a zipped artifact (zip or war for instance) to an existing environment on AWS Elastic Beanstalk
* Deploy a zipped artifact (zip or jar for instance) to an existing function on AWS Lambda
* Deploy a directory to an S3 bucket giving all users read permissions (designed for public S3-hosted websites)
* Create/Update a stack on CloudFormation
* Supports java 7+
* Supports proxy

Status: *released to Maven Central*

[Maven reports](http://davidmoten.github.io/aws-maven-plugin/index.html)

## How to use

### Authentication
To authenticate your activities against AWS you can 
* specify the accessKey and secretAccessKey in the configuration of the plugin, 
**or** 
* use a `serverId` defined in `~/.m2/settings.xml`.

To use a `serverId` you'll need xml like this in your `~/.m2/settings.xml`:

```xml
<server>
    <id>amsa.aws</id>
    <username>AWS_ACCESS_KEY_HERE</username>
    <password>AWS_SECRET_ACCESS_KEY_HERE</password>
</server>
```
Only the password field (secret access key) in the `server` element can be encrypted (as per `mvn -ep`).

### Deploy to Beanstalk
Add this to the `<plugins>` section of your pom.xml:

```xml
<plugin>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>aws-maven-plugin</artifactId>
    <version>[LATEST_VERSION]</version>
    <configuration>
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>${env.AWS_ACCESS_KEY}</awsAccessKey>
        <awsSecretAccessKey>${env.AWS_SECRET_ACCESS_KEY}</awsSecretAccessKey>
        <artifact>${project.build.directory}/my-artifact.war</artifact>
        <applicationName>my-application-name</applicationName>
        <environmentName>my-environment-name</environmentName>
        <region>ap-southeast-2</region>
        <!-- optional versionLabel -->
        <versionLabel>my-artifact-${maven.build.timestamp}.war</versionLabel>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.amsa.gov.au</httpsProxyHost>
        <httpsProxyPort>8080</httpsProxyPort>
        <httpsProxyUsername>user</httpsProxyUsername>
        <httpsProxyPassword>pass</httpsProxyPassword>
    </configuration>
</plugin>
```
Notes:
* If you don't access AWS via an https proxy then leave those configuration settings out.
* You can also specify a `<versionLabel>` in configuration if you want. If you don't it is automatically generated for you using the application name and a timestamp.

To deploy a war and get it running on Beanstalk:

```bash
export AWS_ACCESS_KEY=<your_key>
export AWS_SECRET_ACCESS_KEY=<your_secret>
mvn package aws:deploy
```

The user represented by the AWS access key must have put permission on S3 and full access permission on ElasticBeanstalk.

### Deploy to Lambda
Add this to the `<plugins>` section of your pom.xml:

```xml
<plugin>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>aws-maven-plugin</artifactId>
    <version>[LATEST_VERSION]</version>
    <configuration>
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>${env.AWS_ACCESS_KEY}</awsAccessKey>
        <awsSecretAccessKey>${env.AWS_SECRET_ACCESS_KEY}</awsSecretAccessKey>
        <artifact>${project.build.directory}/my-artifact.war</artifact>
        <functionName>myFunction</functionName>
        <region>ap-southeast-2</region>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.amsa.gov.au</httpsProxyHost>
        <httpsProxyPort>8080</httpsProxyPort>
        <httpsProxyUsername>user</httpsProxyUsername>
        <httpsProxyPassword>pass</httpsProxyPassword>
    </configuration>
</plugin>
```

Notes:
* If you don't access AWS via an https proxy then leave those configuration settings out.
* Adding `AWSLambdaFullAccess` managed policy to your user in IAM doesn't give you the ability to call `UpdateFunctionCode`. To fix this add an inline policy as below:

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "Stmt1464440182000",
            "Effect": "Allow",
            "Action": [
                "lambda:InvokeAsync",
                "lambda:InvokeFunction",
                "lambda:UpdateFunctionCode"
            ],
            "Resource": [
                "*"
            ]
        }
    ]
}
```

To deploy a jar and get it running on Lambda:

```bash
export AWS_ACCESS_KEY=<your_key>
export AWS_SECRET_ACCESS_KEY=<your_secret>
mvn package aws:deployLambda
```
### Deploy directory to S3
* deploys a directory to a path in an S3 bucket
* all uploaded files are given public read permissions
* designed for upload of public websites

Add this to the `<plugins>` section of your pom.xml:

```xml
<plugin>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>aws-maven-plugin</artifactId>
    <version>[LATEST_VERSION]</version>
    <configuration>
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>${env.AWS_ACCESS_KEY}</awsAccessKey>
        <awsSecretAccessKey>${env.AWS_SECRET_ACCESS_KEY}</awsSecretAccessKey>
        <region>ap-southeast-2</region>
        <inputDirectory>src/main/webapp</inputDirectory>
        <bucketName>the_bucket</bucketName>
        <outputBasePath></outputBasePath>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.amsa.gov.au</httpsProxyHost>
        <httpsProxyPort>8080</httpsProxyPort>
        <httpsProxyUsername>user</httpsProxyUsername>
        <httpsProxyPassword>pass</httpsProxyPassword>
    </configuration>
</plugin>
```
Notes:
* If you don't access AWS via an https proxy then leave those configuration settings out.

```bash
export AWS_ACCESS_KEY=<your_key>
export AWS_SECRET_ACCESS_KEY=<your_secret>
mvn package aws:deployS3
```

### Create/Update CloudfFormation stack

To create or update a stack in CloudFormation (bulk create/modify resources in AWS using a declarative definition) specify the name of the stack, the template and its parameters to the plugin as below.

```xml
<plugin>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>aws-maven-plugin</artifactId>
    <version>[LATEST_VERSION]</version>
    <configuration>
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>${env.AWS_ACCESS_KEY}</awsAccessKey>
        <awsSecretAccessKey>${env.AWS_SECRET_ACCESS_KEY}</awsSecretAccessKey>
        <region>ap-southeast-2</region>
        <stackName>myStack</stackName>
        <template>src/main/aws/cloudformation.yaml</template>
        <parameters>
            <mode>dev</mode>
            <version>6.01</version>
        </parameters>
        <intervalSeconds>2</intervalSeconds>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.amsa.gov.au</httpsProxyHost>
        <httpsProxyPort>8080</httpsProxyPort>
        <httpsProxyUsername>user</httpsProxyUsername>
        <httpsProxyPassword>pass</httpsProxyPassword>
    </configuration>
</plugin>
```

and call 

```bash
mvn package aws:deployCf
```

Nice and easy! (Let me know if you have any problems!)
