package com.github.davidmoten.aws.maven;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Optional;

import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;

class LambdaDeployer {

    private final Log log;

    LambdaDeployer(Log log) {
        this.log = log;
    }

    void deploy(AwsKeyPair keyPair, String region, String zipFilename, String functionName, String functionAlias, Proxy proxy) {
        long t = System.currentTimeMillis();
        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        AWSLambda lambda = AWSLambdaClientBuilder.standard().withCredentials(credentials)
                .withClientConfiguration(Util.createConfiguration(proxy)).withRegion(region).build();

        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(new FileInputStream(zipFilename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DecimalFormat df = new DecimalFormat("0.000");
        log.info("deploying " + zipFilename + ", length=" + df.format(bytes.length / 1024.0 / 1024.0)
                + "MB, to functionName=" + functionName);
        UpdateFunctionCodeResult updateFunctionCodeResult = lambda.updateFunctionCode( //
                new UpdateFunctionCodeRequest() //
                        .withFunctionName(functionName) //
                        .withPublish(true) //
                        .withZipFile(ByteBuffer.wrap(bytes)));
        log.info("deployed in " + (System.currentTimeMillis() - t) + "ms");
        Optional<String> optionalFunctionAlias = Optional.ofNullable(functionAlias);
        if (optionalFunctionAlias.isPresent()) {
            CreateAliasResult createAliasResult = lambda.createAlias(
                new CreateAliasRequest()
                    .withFunctionVersion(updateFunctionCodeResult.getVersion())
                    .withFunctionName(functionName)
                    .withName(optionalFunctionAlias.get()));
            log.info("created alias=" + optionalFunctionAlias.get() + " to functionName=" + functionName + " for version=" + updateFunctionCodeResult.getVersion());
        }
    }

}
