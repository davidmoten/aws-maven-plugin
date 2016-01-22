package com.github.davidmoten.aws.maven;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deploy")
public final class DeployMojo extends AbstractMojo {

    @Parameter(property = "awsAccessKey")
    private String awsAccessKey;

    @Parameter(property = "awsSecretAccessKey")
    private String awsSecretAccessKey;

    @Parameter(property = "applicationName")
    private String applicationName;

    @Parameter(property = "environmentName")
    private String environmentName;

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "artifact")
    private File artifact;

    @Parameter(property = "httpsProxyHost")
    private String httpsProxyHost;

    @Parameter(property = "httpsProxyPort")
    private int httpsProxyPort;

    @Parameter(property = "httpsProxyUsername")
    private String httpsProxyUsername;

    @Parameter(property = "httpsProxyPassword")
    private String httpsProxyPassword;

    @Parameter(property = "versionLabel")
    private String versionLabel;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Proxy proxy = new Proxy(httpsProxyHost, httpsProxyPort, httpsProxyUsername,
                httpsProxyPassword);

        if (versionLabel == null) {
            versionLabel = createVersionLabel(applicationName, new Date());
        }

        Deployer deployer = new Deployer(getLog());
        deployer.deploy(artifact, awsAccessKey, awsSecretAccessKey, region, applicationName,
                environmentName, versionLabel, proxy);
    }

    private static String createVersionLabel(String applicationName, Date date) {
        // construct version label using application name and dateTime
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateTime = sdf.format(date);
        return applicationName + "_" + dateTime;
    }

}
