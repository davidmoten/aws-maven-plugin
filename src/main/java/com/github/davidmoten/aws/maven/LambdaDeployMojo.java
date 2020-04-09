package com.github.davidmoten.aws.maven;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deployLambda")
public final class LambdaDeployMojo extends AbstractDeployAwsMojo<AWSLambdaClientBuilder, AWSLambda> {

    @Parameter(property = "functionName")
    private String functionName;

    @Parameter(property = "functionAlias")
    private String functionAlias;

    @Parameter(property = "artifact")
    private String artifact;

    public LambdaDeployMojo() {
        super(AWSLambdaClientBuilder.standard());
    }

    @Override
    protected void execute(AWSLambda lambdaClient) {
        LambdaDeployer deployer = new LambdaDeployer(getLog(), lambdaClient);
        deployer.deploy(artifact, functionName, functionAlias);
    }

}
