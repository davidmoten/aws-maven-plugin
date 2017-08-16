package com.github.davidmoten.aws.maven;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.google.common.base.Preconditions;

final class CloudFormationDeployer {

    private final Log log;

    CloudFormationDeployer(Log log) {
        this.log = log;
    }

    public void deploy(AwsKeyPair keyPair, String region, final String stackName, final String templateBody,
            Map<String, String> parameters, int intervalSeconds, Proxy proxy) {
        Preconditions.checkArgument(intervalSeconds > 0, "intervalSeconds must be greater than 0");
        Preconditions.checkArgument(intervalSeconds <= 600, "intervalSeconds must be less than or equal to 600");

        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonCloudFormation cf = AmazonCloudFormationClientBuilder //
                .standard() //
                .withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();

        Parameter params = new Parameter();
        if (parameters != null) {
            for (Entry<String, String> entry : parameters.entrySet()) {
                params = params //
                        .withParameterKey(entry.getKey()) //
                        .withParameterValue(entry.getValue());
            }
        }

        {
            // list history of application
            log.info("------------------------------");
            log.info("Stack history - " + stackName);
            log.info("------------------------------");
            ListStacksResult r = cf.listStacks();
            r.getStackSummaries() //
                    .stream() //
                    .filter(x -> x.getStackName().equals(stackName)) //
                    .forEach(x -> {
                        log.info("id=" + x.getStackId());
                        log.info("  status=" + x.getStackStatus());
                        log.info("  created=" + x.getCreationTime());
                        log.info("  update=" + x.getLastUpdatedTime());
                        log.info("  deleted=" + x.getDeletionTime());
                    });
        }

        int statusPollingIntervalMs = intervalSeconds * 1000;

        {
            // delete an application in ROLLBACK_COMPLETE status

            ListStacksResult r = cf.listStacks();
            r.getStackSummaries() //
                    .stream() //
                    .filter(x -> x.getStackName().equals(stackName)) //
                    .limit(1) //
                    .filter(x -> StackStatus.ROLLBACK_COMPLETE.toString().equals(x.getStackStatus())) //
                    .forEach(x -> {
                        log.info("Deleting stack with status " + x.getStackStatus()); //
                        cf.deleteStack(new DeleteStackRequest().withStackName(stackName));
                        waitForCompletion(cf, stackName, statusPollingIntervalMs, log);
                    });

        }

        // check if stack exists
        ListStacksResult r = cf.listStacks();

        boolean exists = r.getStackSummaries() //
                .stream() //
                .anyMatch(x -> x.getStackName().equals(stackName)
                        && !StackStatus.DELETE_COMPLETE.name().equals(x.getStackStatus()));

        if (!exists) {
            cf.createStack(new CreateStackRequest() //
                    .withStackName(stackName) //
                    .withTemplateBody(templateBody) //
                    .withParameters(params) //
                    .withCapabilities(Capability.CAPABILITY_IAM) //
                    .withCapabilities(Capability.CAPABILITY_NAMED_IAM));
        } else {
            try {
                cf.updateStack(new UpdateStackRequest() //
                        .withStackName(stackName) //
                        .withTemplateBody(templateBody) //
                        .withParameters(params) //
                        .withCapabilities(Capability.CAPABILITY_IAM) //
                        .withCapabilities(Capability.CAPABILITY_NAMED_IAM));
            } catch (RuntimeException e) {
                log.info(e.getMessage());
                // see https://github.com/hashicorp/terraform/issues/5653
                if (e.getMessage() != null && e.getMessage().contains("ValidationError")
                        && e.getMessage().contains("No updates are to be performed")) {
                    return;
                } else {
                    throw e;
                }
            }
        }
        Status result = waitForCompletion(cf, stackName, statusPollingIntervalMs, log);
        if (!result.value.equals(StackStatus.CREATE_COMPLETE.toString()) //
                && !result.value.equals(StackStatus.UPDATE_COMPLETE.toString())) {
            throw new RuntimeException("create/update failed: " + result);
        }
    }

    private static class Status {
        final String value;
        final String reason;

        Status(String value, String reason) {
            this.value = value;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "Status [value=" + value + ", reason=" + (reason == null ? "" : reason) + "]";
        }

    }

    // Wait for a stack to complete transition
    // End stack states are:
    // CREATE_COMPLETE
    // CREATE_FAILED
    // DELETE_FAILED
    // ROLLBACK_FAILED
    // NO_SUCH_STACK
    public static Status waitForCompletion(AmazonCloudFormation cf, String stackName, int intervalMs, Log log) {

        DescribeStacksRequest describeRequest = new DescribeStacksRequest().withStackName(stackName);
        String stackStatus = "Unknown";
        String stackReason = "";

        log.info("waiting for action on  " + stackName);
        long t = System.currentTimeMillis();

        while (true) {
            List<Stack> stacks;
            try {
                stacks = cf.describeStacks(describeRequest).getStacks();
            } catch (AmazonCloudFormationException e) {
                log.warn(e.getMessage());
                stacks = Collections.emptyList();
            }
            if (stacks.isEmpty()) {
                stackStatus = "NO_SUCH_STACK";
                stackReason = "Stack has been deleted";
                log.info(time(t) + " " + stackStatus);
                break;
            } else {
                // should just be one stack
                Stack stack = stacks.iterator().next();
                String ss = stack.getStackStatus();
                // Show we are waiting
                String sr = stack.getStackStatusReason();
                String msg = ss;
                if (sr != null) {
                    msg = msg + " - " + sr;
                }
                log.info(time(t) + " " + msg);
                if (ss.equals(StackStatus.CREATE_COMPLETE.toString()) || ss.equals(StackStatus.CREATE_FAILED.toString())
                        || ss.equals(StackStatus.UPDATE_COMPLETE.toString())
                        || ss.equals(StackStatus.UPDATE_ROLLBACK_COMPLETE.toString())
                        || ss.equals(StackStatus.UPDATE_ROLLBACK_FAILED.toString())
                        || ss.equals(StackStatus.ROLLBACK_FAILED.toString())
                        || ss.equals(StackStatus.ROLLBACK_COMPLETE.toString())
                        || ss.equals(StackStatus.DELETE_FAILED.toString())
                        || ss.equals(StackStatus.DELETE_COMPLETE.toString())) {
                    stackStatus = ss;
                    stackReason = stack.getStackStatusReason();
                    break;
                }
            }

            // Not done yet so sleep for 10 seconds.
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return new Status(stackStatus, stackReason);
    }

    private static String time(long start) {
        long s = (System.currentTimeMillis() - start) / 1000;
        long a = s / 60;
        long b = s % 60;
        return String.format("%02d:%02d", a, b);
    }

}
