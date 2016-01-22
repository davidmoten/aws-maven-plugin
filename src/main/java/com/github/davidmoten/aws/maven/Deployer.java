package com.github.davidmoten.aws.maven;

import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.github.davidmoten.aws.maven.DeployMojo.Proxy;

public final class Deployer {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private final Log log;

    public Deployer(Log log) {
        this.log = log;
    }

    public void deploy(File artifact, String accessKey, String secretKey, String region,
            String applicationName, String environmentName, Proxy proxy) {

        final AWSCredentialsProvider credentials = new StaticCredentialsProvider(
                new BasicAWSCredentials(accessKey, secretKey));

        final Region r = Region.getRegion(Regions.fromName(region));

        ClientConfiguration cc = createConfiguration(proxy);

        AWSElasticBeanstalkClient eb = new AWSElasticBeanstalkClient(credentials, cc).withRegion(r);
        log.info("getting s3 bucket name to deploy artifact to");
        String bucketName = eb.createStorageLocation().getS3Bucket();
        log.info("s3Bucket=" + bucketName);

        AmazonS3Client s3 = new AmazonS3Client(credentials, cc).withRegion(r);
        String dateTime = Instant.now().atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(DATETIME_PATTERN));
        String objectName = "artifact." + dateTime;
        log.info("uploading " + artifact + " to " + bucketName + ":" + objectName);
        s3.putObject(bucketName, objectName, artifact);

        String versionLabel = applicationName + "-" + dateTime;
        log.info("creating version label=" + versionLabel);
        CreateApplicationVersionRequest request1 = new CreateApplicationVersionRequest()
                .withApplicationName(applicationName).withAutoCreateApplication(true)
                .withSourceBundle(new S3Location(bucketName, objectName))
                .withVersionLabel(versionLabel);
        eb.createApplicationVersion(request1);

        log.info("requesting update of environment to new version label");
        UpdateEnvironmentRequest request2 = new UpdateEnvironmentRequest()
                .withApplicationName(applicationName).withEnvironmentName(environmentName)
                .withVersionLabel(versionLabel);
        eb.updateEnvironment(request2);
        log.info("requested");

    }

    private static ClientConfiguration createConfiguration(Proxy proxy) {
        ClientConfiguration cc = new ClientConfiguration();
        if (proxy.host != null) {
            cc.setProxyHost(proxy.host);
            cc.setProxyPort(proxy.port);
            if (proxy.username != null) {
                cc.setProxyUsername(proxy.username);
                cc.setProxyPassword(proxy.password);
            }
        }
        return cc;
    }

    public static void main(String[] args) {
        System.out.println(Instant.now().atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
    }

}
