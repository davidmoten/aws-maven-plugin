package com.github.davidmoten.aws.maven;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.*;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;

import java.util.Optional;

public abstract class AbstractAwsMojo extends AbstractMojo {

    private static final String REGION_NOT_FOUND_ERROR_MESSAGE = "The region must be specified either in the plugin " +
            "configuration (region), environment variable (AWS_REGION), system property (aws.region), AWS shared " +
            "configuration file or instance metadata.";

    private static final String CREDENTIALS_NOT_FOUND_ERROR_MESSAGE = "The AWS access key and AWS secret access key " +
            "must be specified either in Maven server authentication profile (by specifying serverId in the plugin " +
            "configuration), plugin configuration (awsAccessKey and awsSecretAccessKey), environment variables " +
            "(AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY), system properties (aws.accessKeyId and aws.secretKey), " +
            "default credential profiles file, ECS container credentials or instance profile credentials";

    private static final AwsRegionProvider DEFAULT_REGION_PROVIDER = new DefaultAwsRegionProviderChain();

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
     * An ID of the server authentication profile defined in maven settings.
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

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Component
    private SettingsDecrypter decrypter;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        String region = getRegion()
                .orElseThrow(() -> new MojoExecutionException(REGION_NOT_FOUND_ERROR_MESSAGE));
        AWSCredentialsProvider credentials = getAwsCredentialsProvider()
                .orElseThrow(() -> new MojoExecutionException(CREDENTIALS_NOT_FOUND_ERROR_MESSAGE));

        Proxy proxy = new Proxy(httpsProxyHost, httpsProxyPort, httpsProxyUsername, httpsProxyPassword);
        execute(credentials, region, proxy);
    }

    /**
     * Returns the region configured in the plugin configuration or the one inferred using default region provider
     * chain.
     */
    private Optional<String> getRegion() {
        if (this.region != null) {
            return Optional.of(this.region);
        }

        String region;
        try {
            region = DEFAULT_REGION_PROVIDER.getRegion();
        } catch (SdkClientException e) {
            // Ignoring the exception thrown in case no region is found
            region = null;
        }

        return Optional.ofNullable(region);
    }

    /**
     * Return an {@link AWSCredentialsProvider} by looking the following sources:
     *
     * <ol>
     *     <li>Server authentication profile with the specified in the plugin configuration server id.</li>
     *     <li>{@code awsAccessKey} and {@code awsSecretAccessKey} specified in the plugin configuration.</li>
     *     <li>Using default AWS credential provider chain.</li>
     * </ol>
     */
    private Optional<AWSCredentialsProvider> getAwsCredentialsProvider() {
        AWSCredentials awsCredentials = null;
        if (serverId != null) {
            Server server = settings.getServer(serverId);
            if (server != null) {
                SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
                Server decryptedServer = decrypter.decrypt(request).getServer();
                awsCredentials = new BasicAWSCredentials(decryptedServer.getUsername(), decryptedServer.getPassword());
            } else {
                getLog().warn("Unable to find the server with the following id in the settings: " + serverId);
            }
        }

        if (awsCredentials == null && awsAccessKey != null && awsSecretAccessKey != null) {
            awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretAccessKey);
        }

        if (awsCredentials == null) {
            try {
                awsCredentials = DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
            } catch (SdkClientException e) {
                // Ignore the exception thrown in case no AWS credentials are found
            }
        }

        return Optional.ofNullable(awsCredentials)
                .map(AWSStaticCredentialsProvider::new);
    }

    protected abstract void execute(AWSCredentialsProvider awsCredentials, String region, Proxy proxy)
            throws MojoExecutionException, MojoFailureException;

}
