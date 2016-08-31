package com.github.davidmoten.aws.maven;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;

class LambdaDeployer {

    private final Log log;

    LambdaDeployer(Log log) {
        this.log = log;
    }

    void deploy(String accessKey, String secretKey, String region, String zipFilename,
            String functionName, Proxy proxy) {
        long t = System.currentTimeMillis();
        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKey, secretKey));

        final Region r = Region.getRegion(Regions.fromName(region));

        AWSLambdaClient lambda = new AWSLambdaClient(credentials, Util.createConfiguration(proxy))
                .withRegion(r);

        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(new FileInputStream(zipFilename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DecimalFormat df = new DecimalFormat("0.000");
        log.info(
                "deploying " + zipFilename + ", length=" + df.format(bytes.length / 1024.0 / 1024.0)
                        + "MB, to functionName=" + functionName);
        lambda.updateFunctionCode( //
                new UpdateFunctionCodeRequest() //
                        .withFunctionName(functionName) //
                        .withPublish(true) //
                        .withZipFile(ByteBuffer.wrap(bytes)));
        log.info("deployed in " + (System.currentTimeMillis() - t) + "ms");
    }

}
