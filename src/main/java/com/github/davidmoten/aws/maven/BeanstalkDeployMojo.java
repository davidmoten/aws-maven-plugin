package com.github.davidmoten.aws.maven;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Date;

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

        BeanstalkDeployer deployer = new BeanstalkDeployer(getLog());
        deployer.deploy(credentials, region, artifact, applicationName, environmentName, versionLabel, proxy);
    }

    private static String createVersionLabel(String applicationName, Date date, String version) {
        // construct version label using application name and dateTime
        return applicationName + "_" + version + "_" + Util.formatDateTime(date);
    }

}
