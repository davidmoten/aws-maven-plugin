package com.github.davidmoten.aws.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

@Mojo(name = "deployCf")
public final class CloudFormationDeployMojo extends AbstractMojo {

    @Parameter(property = "awsAccessKey")
    private String awsAccessKey;

    @Parameter(property = "awsSecretAccessKey")
    private String awsSecretAccessKey;

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "serverId")
    private String serverId;

    @Parameter(property = "stackName")
    private String stackName;

    @Parameter(property = "parameters")
    private Map<String, String> parameters;

    @Parameter(property = "stackTemplate")
    private File template;

    @Parameter(property = "templateUrl")
    private String templateUrl;

    @Parameter(property = "intervalSeconds", defaultValue="5")
    private int intervalSeconds;

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
        CloudFormationDeployer deployer = new CloudFormationDeployer(getLog());
        AwsKeyPair keys = Util.getAwsKeyPair(serverId, awsAccessKey, awsSecretAccessKey, settings,
                decrypter);
        byte[] bytes;

        if (templateUrl == null) {
            try {
                bytes = Files.readAllBytes(template.toPath());
            } catch (IOException e) {
                throw new MojoFailureException(
                        "could not read template=" + template + ": " + e.getMessage(), e);
            }
        } else {
            bytes = new byte[0];
        }

        // Note UTF-16 is possible also if starts with byte-order mark, see yaml
        // docs. Not going to worry about detecting UTF-16 until someone
        // complains!
        String templateBody = new String(bytes, StandardCharsets.UTF_8);
        deployer.deploy(keys, region, stackName, templateBody, parameters,
                intervalSeconds, proxy, templateUrl);
    }

}
