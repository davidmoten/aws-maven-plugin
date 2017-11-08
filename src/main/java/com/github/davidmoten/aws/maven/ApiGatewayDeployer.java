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

public class ApiGatewayDeployer {

    private final Log log;

    ApiGatewayDeployer(Log log) {
        this.log = log;
    }

    public void deploy(AwsKeyPair keyPair, String region, final String apiId, final String stage, Proxy proxy) {
        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonApiGateway ag = AmazonApiGatewayClientBuilder.standard().withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();
        log.info("getting deploymentId for " + apiId + ":" + stage);
        GetStageResult s = ag.getStage(new GetStageRequest() //
                .withRestApiId(apiId) //
                .withStageName(stage));
        String deploymentId = s.getDeploymentId();
        log.info("deploymentId=" + deploymentId);
        UpdateDeploymentResult r = ag.updateDeployment(new UpdateDeploymentRequest() //
                .withRestApiId(apiId) //
                .withDeploymentId(deploymentId));
        Map<String, Map<String, MethodSnapshot>> summary = r.getApiSummary();
        log.info("updated deployment");
        log.info("summary=" + summary);
    }

}
