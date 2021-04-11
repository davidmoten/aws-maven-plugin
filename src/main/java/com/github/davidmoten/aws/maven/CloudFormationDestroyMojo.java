package com.github.davidmoten.aws.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;

/**
 * Destroys CloudFormation stack specified in {@link #stackName} parameter.
 *
 * @author Maciej Walkowiak
 */
@Mojo(name = "destroyCf")
public final class CloudFormationDestroyMojo extends AbstractDeployAwsMojo<AmazonCloudFormationClientBuilder, AmazonCloudFormation> {

    @Parameter(property = "stackName")
    private String stackName;

    @Parameter(property = "intervalSeconds", defaultValue="5")
    private int intervalSeconds;

    public CloudFormationDestroyMojo() {
        super(AmazonCloudFormationClientBuilder.standard());
    }

    @Override
    protected void execute(AmazonCloudFormation cloudFormationClient) throws MojoFailureException {
        CloudFormationDeployer deployer = new CloudFormationDeployer(getLog(), cloudFormationClient);
        deployer.destroy(stackName, intervalSeconds);
    }

}
