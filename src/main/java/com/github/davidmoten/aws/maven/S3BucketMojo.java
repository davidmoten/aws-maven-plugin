package com.github.davidmoten.aws.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

@Mojo(name = "createS3Bucket")
public final class S3BucketMojo extends AbstractMojo {

	@Parameter(property = "awsAccessKey")
	private String awsAccessKey;

	@Parameter(property = "awsSecretAccessKey")
	private String awsSecretAccessKey;

	@Parameter(property = "serverId")
	private String serverId;

	@Parameter(property = "region")
	private String region;

	@Parameter(property = "bucketName")
	private String bucketName;

	@Parameter(property = "httpsProxyHost")
	private String httpsProxyHost;

	@Parameter(property = "httpsProxyPort")
	private int httpsProxyPort;

	@Parameter(property = "httpsProxyUsername")
	private String httpsProxyUsername;

	@Parameter(property = "httpsProxyPassword")
	private String httpsProxyPassword;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings settings;

	@Component
	private SettingsDecrypter decrypter;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		Proxy proxy = new Proxy(httpsProxyHost, httpsProxyPort, httpsProxyUsername, httpsProxyPassword);

		S3Bucket bucket = new S3Bucket(getLog());
		AwsKeyPair keyPair = Util.getAwsKeyPair(serverId, awsAccessKey, awsSecretAccessKey, settings, decrypter);
		bucket.create(keyPair, region, bucketName, proxy);
	}

}
