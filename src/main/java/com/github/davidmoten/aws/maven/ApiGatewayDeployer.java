package com.github.davidmoten.aws.maven;

import java.util.Map;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.GetStageRequest;
import com.amazonaws.services.apigateway.model.GetStageResult;
import com.amazonaws.services.apigateway.model.MethodSnapshot;
import com.amazonaws.services.apigateway.model.UpdateDeploymentRequest;
import com.amazonaws.services.apigateway.model.UpdateDeploymentResult;
import com.google.common.base.Preconditions;

public class ApiGatewayDeployer {

    private final Log log;

    ApiGatewayDeployer(Log log) {
        this.log = log;
    }

    public void deploy(AwsKeyPair keyPair, String region, final String apiId, final String stage,
            int intervalSeconds, Proxy proxy) {
        long startTime = System.currentTimeMillis();
        Preconditions.checkArgument(intervalSeconds > 0, "intervalSeconds must be greater than 0");
        Preconditions.checkArgument(intervalSeconds <= 600,
                "intervalSeconds must be less than or equal to 600");

        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonApiGateway ag = AmazonApiGatewayClientBuilder.standard().withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();
        GetStageResult s = ag.getStage(new GetStageRequest() //
                .withRestApiId(apiId) //
                .withStageName(stage));
        String deploymentId = s.getDeploymentId();
        UpdateDeploymentResult r = ag.updateDeployment(new UpdateDeploymentRequest() //
                .withRestApiId(apiId) //
                .withDeploymentId(deploymentId));
        Map<String, Map<String, MethodSnapshot>> summary = r.getApiSummary();
        log.info(summary.toString());
    }

}
