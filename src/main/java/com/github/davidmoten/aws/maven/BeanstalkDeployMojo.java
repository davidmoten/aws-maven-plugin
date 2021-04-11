package com.github.davidmoten.aws.maven;

import java.io.File;
import java.util.Date;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Mojo(name = "deploy")
public final class BeanstalkDeployMojo extends AbstractAwsMojo {

    @Parameter(property = "applicationName")
    private String applicationName;

    @Parameter(property = "environmentName")
    private String environmentName;

    @Parameter(property = "artifact")
    private File artifact;

    @Parameter(property = "versionLabel")
    private String versionLabel;

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Override
    protected void execute(AWSCredentialsProvider credentials, String region, Proxy proxy) {
        if (versionLabel == null) {
            versionLabel = createVersionLabel(applicationName, new Date(), project.getVersion());
        }
        ClientConfiguration clientConfiguration = Util.createConfiguration(proxy);
        AWSElasticBeanstalk beanstalkClient = AWSElasticBeanstalkClientBuilder.standard()
                .withRegion(region)
                .withCredentials(credentials)
                .withClientConfiguration(clientConfiguration)
                .build();
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(credentials)
                .withClientConfiguration(clientConfiguration)
                .build();
        BeanstalkDeployer deployer = new BeanstalkDeployer(getLog(), beanstalkClient, s3Client);
        deployer.deploy(artifact, applicationName, environmentName, versionLabel);
    }

    private static String createVersionLabel(String applicationName, Date date, String version) {
        // construct version label using application name and dateTime
        return applicationName + "_" + version + "_" + Util.formatDateTime(date);
    }

}
