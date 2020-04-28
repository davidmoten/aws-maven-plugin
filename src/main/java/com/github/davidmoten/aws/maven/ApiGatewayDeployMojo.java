package com.github.davidmoten.aws.maven;

import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deployRestApi")
public final class ApiGatewayDeployMojo extends AbstractDeployAwsMojo<AmazonApiGatewayClientBuilder, AmazonApiGateway> {

    @Parameter(property = "stage")
    private String stage;

    @Parameter(property = "restApiName")
    private String restApiName;

    public ApiGatewayDeployMojo() {
        super(AmazonApiGatewayClientBuilder.standard());
    }

    @Override
    protected void execute(AmazonApiGateway apiGatewayClient) {
        ApiGatewayDeployer deployer = new ApiGatewayDeployer(getLog(), apiGatewayClient);
        deployer.deploy(restApiName, stage);
    }
}
