package com.github.davidmoten.aws.maven;

import java.util.Map;
import java.util.Optional;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.model.CreateDeploymentRequest;
import com.amazonaws.services.apigateway.model.CreateDeploymentResult;
import com.amazonaws.services.apigateway.model.GetRestApisRequest;
import com.amazonaws.services.apigateway.model.GetRestApisResult;
import com.amazonaws.services.apigateway.model.MethodSnapshot;
import com.amazonaws.services.apigateway.model.RestApi;

final class ApiGatewayDeployer {

    private final Log log;
    private final AmazonApiGateway apiGatewayClient;

    ApiGatewayDeployer(Log log, AmazonApiGateway apiGatewayClient) {
        this.log = log;
        this.apiGatewayClient = apiGatewayClient;
    }

    public void deploy(String restApiName, String stage) {
        GetRestApisResult apis = apiGatewayClient.getRestApis(new GetRestApisRequest().withLimit(10000));
        Optional<RestApi> api = apis.getItems().stream().filter(item -> item.getName().equals(restApiName)).findFirst();
        RestApi a = api.orElseThrow(() -> new RuntimeException("no rest api found with name='" + restApiName + "'"));
        String restApiId = a.getId();
        log.info("creating deployment of " + restApiId + " to stage " + stage);
        CreateDeploymentResult r = apiGatewayClient
                .createDeployment(new CreateDeploymentRequest().withRestApiId(restApiId).withStageName(stage));
        Map<String, Map<String, MethodSnapshot>> summary = r.getApiSummary();
        log.info("created deployment");
        log.info("summary=" + summary);
    }

}
