package com.github.davidmoten.aws.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "deployLambda")
public final class LambdaDeployMojo extends AbstractMojo {

    @Parameter(property = "awsAccessKey")
    private String awsAccessKey;

    @Parameter(property = "awsSecretAccessKey")
    private String awsSecretAccessKey;

    @Parameter(property = "functionName")
    private String functionName;

    @Parameter(property = "zipFilename")
    private String zipFilename;

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "httpsProxyHost")
    private String httpsProxyHost;

    @Parameter(property = "httpsProxyPort")
    private int httpsProxyPort;

    @Parameter(property = "httpsProxyUsername")
    private String httpsProxyUsername;

    @Parameter(property = "httpsProxyPassword")
    private String httpsProxyPassword;

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Proxy proxy = new Proxy(httpsProxyHost, httpsProxyPort, httpsProxyUsername,
                httpsProxyPassword);
        LambdaDeployer deployer = new LambdaDeployer(getLog());
        deployer.deploy(awsAccessKey, awsSecretAccessKey, region, zipFilename, functionName, proxy);
    }
}
