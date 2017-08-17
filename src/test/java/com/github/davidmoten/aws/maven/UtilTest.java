package com.github.davidmoten.aws.maven;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UtilTest {

    @Test
    public void testFormatJson() {
        String s = "{\"ApplicationName\":\"inmarsat-c-receiver\",\"Description\":\"inmarsat-c-receiver Configuration Template\",\"OptionSettings\":[{\"Value\":\"t2.nano\",\"Namespace\":\"aws:autoscaling:launchconfiguration\",\"OptionName\":\"InstanceType\"},{\"Value\":\"SingleInstance\",\"Namespace\":\"aws:elasticbeanstalk:environment\",\"OptionName\":\"EnvironmentType\"},{\"Value\":\"inmarsat-c-receiver-dev-ApplicationInstanceProfile-LZ7S1G4YZZNM\",\"Namespace\":\"aws:autoscaling:launchconfiguration\",\"OptionName\":\"IamInstanceProfile\"},{\"Value\":\"inmarsat-c-receiver-service-role-dev\",\"Namespace\":\"aws:elasticbeanstalk:environment\",\"OptionName\":\"ServiceRole\"},{\"Value\":\"inmarsat-c-receiver-security-group-dev\",\"Namespace\":\"aws:autoscaling:launchconfiguration\",\"OptionName\":\"SecurityGroups\"},{\"Value\":\"amsa\",\"Namespace\":\"aws:autoscaling:launchconfiguration\",\"OptionName\":\"EC2KeyName\"},{\"Value\":\"-Dsqs.queue.name=inmarsat-c-receiver-in-dev -Ds3.bucket.name=inmarsat-c-receiver-in-dev\",\"Namespace\":\"aws:elasticbeanstalk:container:tomcat:jvmoptions\",\"OptionName\":\"JVM Options\"},{\"Value\":\"david.moten@amsa.gov.au\",\"Namespace\":\"aws:elasticbeanstalk:sns:topics\",\"OptionName\":\"Notification Endpoint\"},{\"Value\":\"email\",\"Namespace\":\"aws:elasticbeanstalk:sns:topics\",\"OptionName\":\"Notification Protocol\"},{\"Value\":\"true\",\"Namespace\":\"aws:elasticbeanstalk:managedactions\",\"OptionName\":\"ManagedActionsEnabledz\"},{\"Value\":\"TUE:10:00\",\"Namespace\":\"aws:elasticbeanstalk:managedactions\",\"OptionName\":\"PreferredStartTime\"},{\"Value\":\"patch\",\"Namespace\":\"aws:elasticbeanstalk:managedactions:platformupdate\",\"OptionName\":\"UpdateLevel\"},{\"Value\":\"true\",\"Namespace\":\"aws:elasticbeanstalk:managedactions:platformupdate\",\"OptionName\":\"InstanceRefreshEnabled\"},{\"Value\":\"enhanced\",\"Namespace\":\"aws:elasticbeanstalk:healthreporting:system\",\"OptionName\":\"SystemType\"},{\"Value\":\"true\",\"Namespace\":\"aws:elasticbeanstalk:hostmanager\",\"OptionName\":\"LogPublicationControl\"},{\"Value\":\"2\",\"Namespace\":\"aws:autoscaling:asg\",\"OptionName\":\"MinSize\"},{\"Value\":\"4\",\"Namespace\":\"aws:autoscaling:asg\",\"OptionName\":\"MaxSize\"}],\"SolutionStackName\":\"64bit Amazon Linux 2017.03 v2.6.3 running Tomcat 8 Java 8\"}";
        assertTrue(Util.formatJson(s).contains("ApplicationName"));
    }
}
