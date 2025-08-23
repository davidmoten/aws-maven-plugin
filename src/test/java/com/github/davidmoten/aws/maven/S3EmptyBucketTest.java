package com.github.davidmoten.aws.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.junit.Test;

public final class S3EmptyBucketTest {

    @Test
    public void testExcludeObjectFromDelete() {

        String key = "foo/bar/test.jpg";

        List<String> regexes = Arrays.asList(
            "\\\\*.jpg"
        );

        boolean result = S3EmptyBucket.excludeObjectFromDelete(key, regexes);

        assertTrue(result);

        List<String> regexes2 = Arrays.asList(
            "bar/*"
        );

        boolean result2 = S3EmptyBucket.excludeObjectFromDelete(key, regexes2);

        assertTrue(result2);
    }

    @Test
    public void testFilterS3Objects() {

        List<String> regexes = Arrays.asList(
            "config/*"
        );

        S3ObjectSummary o1 = new S3ObjectSummary();
        o1.setKey("config/config.json");
        o1.setBucketName("test");

        S3ObjectSummary o2 = new S3ObjectSummary();
        o2.setKey("a.js");
        o2.setBucketName("test");

        S3ObjectSummary o3 = new S3ObjectSummary();
        o3.setKey("b.html");
        o3.setBucketName("test");

        S3ObjectSummary o4 = new S3ObjectSummary();
        o4.setKey("asset/a.jpg");
        o4.setBucketName("test");

        S3ObjectSummary o5 = new S3ObjectSummary();
        o5.setKey("config/other.json");
        o5.setBucketName("test");

        List<S3ObjectSummary> summaries = Arrays.asList(
            o1, o2, o3, o4, o5
        );

        List<KeyVersion> keyVersions = S3EmptyBucket.filterS3Objects(summaries, regexes);

        List<String> expected = Arrays.asList(
            "a.js",
            "b.html",
            "asset/a.jpg"
        );

        for (KeyVersion kv: keyVersions) {
            assertTrue(expected.contains(kv.getKey()));
        }

        assertEquals(expected.size(), keyVersions.size());

    }


}
