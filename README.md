aws-maven-plugin
-----------------

<a href="https://travis-ci.org/davidmoten/aws-maven-plugin"><img src="https://travis-ci.org/davidmoten/aws-maven-plugin.svg"/></a><br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/aws-maven-plugin/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/aws-maven-plugin)<br/>
<!--[![Dependency Status](https://gemnasium.com/com.github.davidmoten/aws-maven-plugin.svg)](https://gemnasium.com/com.github.davidmoten/aws-maven-plugin)-->

* Deploy a zipped artifact (zip or war for instance) to an environment on AWS Elastic Beanstalk
* Deploy a zipped artifact (zip or jar for instance) to a function on AWS Lambda
* Deploy a directory to an S3 bucket giving all users read permissions (designed for public S3-hosted websites)
* Create/Update a stack on CloudFormation
* Deploy an API Gateway Rest API (CloudFormation does not deploy an api to a stage)
* Supports java 7+
* Supports proxy

Status: *released to Maven Central*

[Maven reports](http://davidmoten.github.io/aws-maven-plugin/index.html)

## How to use

### Authentication

You must provide credentials in order to make requests to AWS services. You can either specify the
credentials in the plugin configuration or rely on the default credential provider chain, which 
attemps to find the credentials in different sources. The followin order is used to find the AWS 
credentials:
1.  If `serverId` is specified, the plugin checks the Maven server authentication profile. In that 
    case your `~/.m2/settings.xml` has to include AWS access keys. In the `servers` tag, add a 
    child `server` tag with an `id` with the `serverId` you specified earlier in the plugin 
    configuration. Use `username` and `password` to define your AWS access and AWS secret access 
    keys respectively:
    ```xml
    <server>
        <id>mycompany.aws</id>
        <username>AWS_ACCESS_KEY_HERE</username>
        <password>AWS_SECRET_ACCESS_KEY_HERE</password>
    </server>
    ```
    Only the password field (secret access key) in the `server` element can be encrypted (as per `mvn -ep`).
2.  Plugin configuration – `awsAccessKey` and `awsSecretAccessKey` parameters.
3.  Default AWS credential provider chain:
    1. Environment variables – `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.
    2. Java system properties – `aws.accessKeyId` and `aws.secretKey`.
    3. The default credential profiles file, that is usually located at `~/.aws/credentials`
    4. Amazon ECS container credentials.
    5. Instance profile credentials.
    6. Web Identity Token credentials from the environment or container. 

### Deploy to Beanstalk
Add this to the `<plugins>` section of your pom.xml:

```xml
<plugin>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>aws-maven-plugin</artifactId>
    <version>[LATEST_VERSION]</version>
    <configuration>
        <!-- Optional authentication configuration. The default credential provider chain is used if the configuration is omitted -->
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>YOUR_AWS_ACCESS_KEY</awsAccessKey>
        <awsSecretAccessKey>YOUR_AWS_SECRET_ACCESS_KEY</awsSecretAccessKey>
        
        <!-- The default region provider chain is used if the region is omitted -->
        <region>ap-southeast-2</region>
        
        <artifact>${project.build.directory}/my-artifact.war</artifact>
        <applicationName>my-application-name</applicationName>
        <environmentName>my-environment-name</environmentName>
        <!-- optional versionLabel -->
        <versionLabel>my-artifact-${maven.build.timestamp}.war</versionLabel>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.me.com</httpsProxyHost>
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
        <!-- Optional authentication configuration. The default credential provider chain is used if the configuration is omitted -->
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>YOUR_AWS_ACCESS_KEY</awsAccessKey>
        <awsSecretAccessKey>YOUR_AWS_SECRET_ACCESS_KEY</awsSecretAccessKey>
        
        <!-- The default region provider chain is used if the region is omitted -->
        <region>ap-southeast-2</region>
        
        <artifact>${project.build.directory}/my-artifact.war</artifact>
        <functionName>myFunction</functionName>
        <!-- optional functionAlias, if included an alias for the new lambda version is created -->
        <functionAlias>${project.version}-${maven.build.timestamp}</functionAlias>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.mycompany</httpsProxyHost>
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
* all uploaded files are given public read permissions (can configure this off)
* designed for upload of public websites

Add this to the `<plugins>` section of your pom.xml:

```xml
<plugin>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>aws-maven-plugin</artifactId>
    <version>[LATEST_VERSION]</version>
    <configuration>
        <!-- Optional authentication configuration. The default credential provider chain is used if the configuration is omitted -->
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>YOUR_AWS_ACCESS_KEY</awsAccessKey>
        <awsSecretAccessKey>YOUR_AWS_SECRET_ACCESS_KEY</awsSecretAccessKey>
        
        <!-- The default region provider chain is used if the region is omitted -->
        <region>ap-southeast-2</region>
        
        <inputDirectory>src/main/webapp</inputDirectory>

        <!-- if false uses bucket default ACL -->
        <!-- default is true -->
        <publicRead>false</publicRead>

        <bucketName>the_bucket</bucketName>
        <outputBasePath></outputBasePath>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.mycompany</httpsProxyHost>
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
        <!-- Optional authentication configuration. The default credential provider chain is used if the configuration is omitted -->
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>YOUR_AWS_ACCESS_KEY</awsAccessKey>
        <awsSecretAccessKey>YOUR_AWS_SECRET_ACCESS_KEY</awsSecretAccessKey>
        
        <!-- The default region provider chain is used if the region is omitted -->
        <region>ap-southeast-2</region>
        
        <stackName>myStack</stackName>
        <template>src/main/aws/cloudformation.yaml</template>
        <!--
        or use already uploaded s3 artifact
        <templateUrl>https://bucketName.s3.amazonaws.com/filename.yml</templateUrl>
        -->
        <parameters>
            <mode>dev</mode>
            <version>6.01</version>
        </parameters>
        <intervalSeconds>2</intervalSeconds>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.mycompany</httpsProxyHost>
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

### Deploy an API Gateway API to a Stage

Use the `deployRestApi` goal:

```xml
<plugin>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>aws-maven-plugin</artifactId>
    <version>[LATEST_VERSION]</version>
    <configuration>
        <!-- Optional authentication configuration. The default credential provider chain is used if the configuration is omitted -->
        <!-- if you have serverId then exclude awsAccessKey and awsSecretAccessKey parameters -->
        <serverId>aws</serverId>
        <!-- if you omit serverId then put explicit keys here as below -->
        <awsAccessKey>YOUR_AWS_ACCESS_KEY</awsAccessKey>
        <awsSecretAccessKey>YOUR_AWS_SECRET_ACCESS_KEY</awsSecretAccessKey>
        
        <!-- The default region provider chain is used if the region is omitted -->
        <region>ap-southeast-2</region>
        
        <restApiName>my-gateway</restApiName>
        <stage>dev</stage>
        <!-- optional proxy config -->
        <httpsProxyHost>proxy.mycompany</httpsProxyHost>
        <httpsProxyPort>8080</httpsProxyPort>
        <httpsProxyUsername>user</httpsProxyUsername>
        <httpsProxyPassword>pass</httpsProxyPassword>
    </configuration>
</plugin>
```

and call 

```bash
mvn package aws:deployRestApi
```

Nice and easy! (Let me know if you have any problems!)
