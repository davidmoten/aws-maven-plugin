package com.github.davidmoten.aws.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

@Mojo(name = "deployRestApi")
public final class ApiGatewayDeployMojo extends AbstractMojo {

    @Parameter(property = "awsAccessKey")
    private String awsAccessKey;

    @Parameter(property = "awsSecretAccessKey")
    private String awsSecretAccessKey;

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "serverId")
    private String serverId;

    @Parameter(property = "stage")
    private String stage;

    @Parameter(property = "restApiName")
    private String restApiName;
    
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

    @Parameter(defaultValue="${settings}", readonly=true)
    private Settings settings;

    @Component
    private SettingsDecrypter decrypter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Proxy proxy = new Proxy(httpsProxyHost, httpsProxyPort, httpsProxyUsername,
                httpsProxyPassword);
        ApiGatewayDeployer deployer = new ApiGatewayDeployer(getLog());
        AwsKeyPair keys = Util.getAwsKeyPair(serverId, awsAccessKey, awsSecretAccessKey, settings,
                decrypter);
        deployer.deploy(keys, region, restApiName, stage, proxy);
    }

}
