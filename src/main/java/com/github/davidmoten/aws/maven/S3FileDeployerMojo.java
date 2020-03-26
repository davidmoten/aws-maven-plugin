package com.github.davidmoten.aws.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

@Mojo(name = "deployFileS3")
public final class S3FileDeployerMojo extends AbstractMojo {

    @Parameter(property = "awsAccessKey")
    private String awsAccessKey;

    @Parameter(property = "awsSecretAccessKey")
    private String awsSecretAccessKey;

    @Parameter(property = "serverId")
    private String serverId;

    @Parameter(property = "region")
    private String region;

    /**
     * Name of the bucket to which the file will be deployed.
     */
    @Parameter(property = "bucketName")
    private String bucketName;

    /**
     * The file that will be deployed to S3.
     */
    @Parameter(property = "file", required = true)
    private File file;

    /**
     * The key name of the object in the bucket. The file name will be used by default if the parameter is not
     * configured.
     */
    @Parameter(property = "objectName")
    private String objectName;

    /**
     * Creates a bucket with the given name if it doesn't exist.
     */
    @Parameter(property = "create", defaultValue = "false")
    private boolean create;

    @Parameter(property = "httpsProxyHost")
    private String httpsProxyHost;

    @Parameter(property = "httpsProxyPort")
    private int httpsProxyPort;

    @Parameter(property = "httpsProxyUsername")
    private String httpsProxyUsername;

    @Parameter(property = "httpsProxyPassword")
    private String httpsProxyPassword;

    @Parameter(property = "awsKmsKeyId")
    private String awsKmsKeyId;

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Component
    private SettingsDecrypter decrypter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Proxy proxy = new Proxy(httpsProxyHost, httpsProxyPort, httpsProxyUsername, httpsProxyPassword);

        S3FileDeployer deployer = new S3FileDeployer(getLog());
        AwsKeyPair keyPair = Util.getAwsKeyPair(serverId, awsAccessKey, awsSecretAccessKey, settings, decrypter);
        if (objectName == null) {
            objectName = file.getName();
        }
        deployer.deploy(keyPair, region, file, bucketName, objectName, proxy, create, awsKmsKeyId);
    }

}
