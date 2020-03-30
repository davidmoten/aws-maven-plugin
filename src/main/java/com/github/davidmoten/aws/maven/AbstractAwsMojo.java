package com.github.davidmoten.aws.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

public abstract class AbstractAwsMojo extends AbstractMojo {

    /**
     * AWS access key.
     */
    @Parameter(property = "awsAccessKey")
    private String awsAccessKey;


    /**
     * AWS secret access key.
     */
    @Parameter(property = "awsSecretAccessKey")
    private String awsSecretAccessKey;

    /**
     * AWS region.
     */
    @Parameter(property = "region")
    private String region;

    /**
     * An ID of the server authentication profile defined in maven settings. This parameter is mandatory if access key
     * and secret key are not specified by awsAccessKey and awsSecretAccessKey parameters.
     */
    @Parameter(property = "serverId")
    private String serverId;

    /**
     * An optional proxy host that will be used to connect to AWS.
     */
    @Parameter(property = "httpsProxyHost")
    private String httpsProxyHost;

    /**
     * An optional proxy port to use if connecting through a proxy.
     */
    @Parameter(property = "httpsProxyPort")
    private int httpsProxyPort;

    /**
     * An optional proxy user name to use if connecting through a proxy.
     */
    @Parameter(property = "httpsProxyUsername")
    private String httpsProxyUsername;

    /**
     * An optional proxy user password to use if connecting through a proxy.
     */
    @Parameter(property = "httpsProxyPassword")
    private String httpsProxyPassword;

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Component
    private SettingsDecrypter decrypter;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        Proxy proxy = new Proxy(httpsProxyHost, httpsProxyPort, httpsProxyUsername, httpsProxyPassword);
        AwsKeyPair keyPair = Util.getAwsKeyPair(serverId, awsAccessKey, awsSecretAccessKey, settings, decrypter);
        execute(keyPair, region, proxy);
    }

    protected abstract void execute(AwsKeyPair keyPair, String region, Proxy proxy)
            throws MojoExecutionException, MojoFailureException;

}
