aws-maven-plugin
-----------------

Easily deploy a zipped artifact (zip or war for instance) to Elastic Beanstalk on AWS using maven.

Status: *released to Maven Central*

##How to use
Add this to the `<plugins>` section of your pom.xml:

```xml
<plugin>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>aws-maven-plugin</artifactId>
    <version>0.1</version>
    <configuration>
        <awsAccessKey>${env.AWS_ACCESS_KEY}</awsAccessKey>
        <awsSecretAccessKey>${env.AWS_SECRET_ACCESS_KEY}</awsSecretAccessKey>
        <artifact>${project.build.directory}/my-artifact.war</artifact>
        <applicationName>my-application-name</applicationName>
        <environmentName>my-environment-name</environmentName>
        <region>ap-southeast-2</region>
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

Nice and easy! (Let me know if you have any problems!)
