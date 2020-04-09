package com.github.davidmoten.aws.maven;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
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

    public void deploy(AWSCredentialsProvider credentials, String region, String stackName, String templateBody,
                       Map<String, String> parameters, int intervalSeconds, Proxy proxy, String templateUrl) {
        long startTime = System.currentTimeMillis();
        Preconditions.checkArgument(intervalSeconds > 0, "intervalSeconds must be greater than 0");
        Preconditions.checkArgument(intervalSeconds <= 600, "intervalSeconds must be less than or equal to 600");

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonCloudFormation cf = AmazonCloudFormationClientBuilder //
                .standard() //
                .withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();

        final List<Parameter> params = buildParameters(parameters);

        displayStatusHistory(stackName, cf);

        int statusPollingIntervalMs = intervalSeconds * 1000;

        deleteFailedCreate(stackName, cf, statusPollingIntervalMs);

        boolean exists;
        try {
            exists = !cf.describeStacks( //
                    new DescribeStacksRequest().withStackName(stackName)) //
                    .getStacks() //
                    .isEmpty();
        } catch (AmazonCloudFormationException e) {
            exists = false;
        }
        if (!exists) {
            CreateStackRequest createStackRequest = new CreateStackRequest() //
                    .withStackName(stackName) //
                    .withParameters(params) //
                    .withCapabilities(Capability.CAPABILITY_IAM) //
                    .withCapabilities(Capability.CAPABILITY_AUTO_EXPAND) //
                    .withCapabilities(Capability.CAPABILITY_NAMED_IAM);
            if (templateUrl != null) {
                createStackRequest = createStackRequest.withTemplateURL(templateUrl); //
            } else {
                createStackRequest = createStackRequest.withTemplateBody(templateBody); //
            }
            cf.createStack(createStackRequest);
            log.info("sent createStack command");
        } else {
            try {
                UpdateStackRequest updateStackRequest = new UpdateStackRequest() //
                        .withStackName(stackName) //
                        .withParameters(params) //
                        .withCapabilities(Capability.CAPABILITY_IAM) //
                        .withCapabilities(Capability.CAPABILITY_AUTO_EXPAND) //
                        .withCapabilities(Capability.CAPABILITY_NAMED_IAM);
                if (templateUrl != null) {
                    updateStackRequest = updateStackRequest.withTemplateURL(templateUrl); //
                } else {
                    updateStackRequest = updateStackRequest.withTemplateBody(templateBody); //
                }
                cf.updateStack(updateStackRequest);
                log.info("sent updateStack command");
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
        // insert blank line into log
        log.info("");
        Status result = waitForCompletion(cf, stackName, statusPollingIntervalMs, log);

        // write out recent events
        displayEvents(stackName, cf, startTime);

        if (!result.value.equals(StackStatus.CREATE_COMPLETE.toString()) //
                && !result.value.equals(StackStatus.UPDATE_COMPLETE.toString())) {
            throw new RuntimeException("create/update failed: " + result);
        }
    }

    private void deleteFailedCreate(final String stackName, AmazonCloudFormation cf, int statusPollingIntervalMs) {
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
    }

    private List<Parameter> buildParameters(Map<String, String> parameters) {
        final List<Parameter> params;
        if (parameters != null) {
            params = parameters //
                    .entrySet() //
                    .stream() //
                    .map(p -> new Parameter().withParameterKey(p.getKey()).withParameterValue(p.getValue())) //
                    .collect(Collectors.toList());
        } else {
            params = Collections.emptyList();
        }
        return params;
    }

    private void displayStatusHistory(final String stackName, AmazonCloudFormation cf) {
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
            log.info("");
        }
    }

    private void displayEvents(final String stackName, AmazonCloudFormation cf, long sinceTime) {
        // list history of application
        log.info("------------------------------");
        log.info("Event history - " + stackName);
        log.info("------------------------------");
        DescribeStackEventsResult r = cf.describeStackEvents(new DescribeStackEventsRequest().withStackName(stackName));
        r.getStackEvents() //
                .stream() //
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp())) //
                .filter(x -> x.getTimestamp().getTime() >= sinceTime - TimeUnit.MINUTES.toMillis(1)) //
                .forEach(x -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddd HH:mm:ss");
                    log.info(sdf.format(x.getTimestamp()) + " " + x.getResourceStatus() + " " + x.getResourceType());
                    if (x.getResourceStatusReason() != null) {
                        log.info("  reason=" + x.getResourceStatusReason());
                        if (x.getResourceProperties() != null) {
                            log.info("  properties=\n");
                            log.info(Util.formatJson(x.getResourceProperties()));
                        }
                    }
                });
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
    private static Status waitForCompletion(AmazonCloudFormation cf, String stackName, int intervalMs, Log log) {

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
