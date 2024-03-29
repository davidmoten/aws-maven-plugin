package com.github.davidmoten.aws.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;

@Mojo(name = "deployCf")
public final class CloudFormationDeployMojo extends AbstractDeployAwsMojo<AmazonCloudFormationClientBuilder, AmazonCloudFormation> {

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

    public CloudFormationDeployMojo() {
        super(AmazonCloudFormationClientBuilder.standard());
    }

    @Override
    protected void execute(AmazonCloudFormation cloudFormationClient) throws MojoFailureException {
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

        CloudFormationDeployer deployer = new CloudFormationDeployer(getLog(), cloudFormationClient);
        deployer.deploy(stackName, templateBody, parameters, intervalSeconds, templateUrl);
    }

}
