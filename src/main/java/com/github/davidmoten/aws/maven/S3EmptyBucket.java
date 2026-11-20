package com.github.davidmoten.aws.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.annotations.VisibleForTesting;

import org.apache.maven.plugin.logging.Log;

final class S3EmptyBucket {
    
    private final Log log;
    private final AmazonS3 s3Client;

    S3EmptyBucket(Log log, AmazonS3 s3Client) {
        this.log = log;
        this.s3Client = s3Client;
    }

    public void empty(String bucketName, List<String> excludes, boolean isDryRun, int maxKeys) {
        

        /* 
        * Successfully return if the bucket does not exist so that other
        * maven jobs can create the bucket and/or upload content to it first.
        */ 
        if (!s3Client.doesBucketExistV2(bucketName)) {
            return;
        }
        
        validateRegex(excludes);
        
        try {

            /* S3 OBJECT FETCHING */

            List<KeyVersion> bucketObjectKeys = new ArrayList<>();
            int excludedObjectCount = 0;
            
            // Request 30 objects at a time from the bucket
            ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withMaxKeys(maxKeys);

            ListObjectsV2Result result;

            do {
                result = s3Client.listObjectsV2(req);

                List<S3ObjectSummary> summaries = result.getObjectSummaries();

                // filter + map the keys we want to delete based on exclusion list
                List<KeyVersion> keys = filterS3Objects(summaries, excludes);

                // count how many objects were excluded.
                excludedObjectCount += summaries.size() - keys.size();

                bucketObjectKeys.addAll(keys);
                
                // If there are more than maxKeys keys in the bucket, get a continuation token
                // and list the next objects.
                String token = result.getNextContinuationToken();
                req.setContinuationToken(token);

            } while (result.isTruncated());
            
            log.info(String.format("Found %d objects (excluding %d) from bucket %s. ", bucketObjectKeys.size(), excludedObjectCount, bucketName));

            /* S3 OBJECT DELETION */

            if (bucketObjectKeys.isEmpty()) {
                log.info("No objects to remove from bucket " + bucketName + "!");
            } else {
                if (!isDryRun) {

                    // Delete the objects.
                    DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(bucketName)
                        .withKeys(bucketObjectKeys)
                        .withQuiet(false);

                    // Verify that the objects were deleted successfully.
                    DeleteObjectsResult delObjRes = s3Client.deleteObjects(multiObjectDeleteRequest);
                    int successfulDeletes = delObjRes.getDeletedObjects().size();
                    log.info(successfulDeletes + " objects successfully deleted.");
                } else {
                    log.info("[Dry Run] Deleting the following objects:");
                    for (KeyVersion kv: bucketObjectKeys) {
                        log.info(String.format("[Dry Run] - will delete %s/%s", bucketName, kv.getKey()));
                    }   
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void validateRegex(List<String> regexes) {

        if (regexes == null) {
            return;
        }
        
        for (String regex: regexes) {
            try {
                Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                throw new RuntimeException("Invalid regular expression: " + regex);
            }
        }
    }

    @VisibleForTesting
    static boolean excludeObjectFromDelete(String objKey, List<String> excludes) {

        if (excludes == null) {
            return false;
        }

        // check the exclusion regexes
        for (String exclude: excludes) {
            final Matcher m = Pattern.compile(exclude).matcher(objKey);
            if (m.find()) {
                return true;
            }
        }

        return false;
    }

    @VisibleForTesting
    static List<KeyVersion> filterS3Objects(List<S3ObjectSummary> objects, List<String> excludes){
        return objects
                .stream()
                .filter(s -> !excludeObjectFromDelete(s.getKey(), excludes))
                .map(s -> new KeyVersion(s.getKey()))
                .collect(Collectors.toList());
    }
}
