package com.github.davidmoten.aws.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;

final class S3Deployer {

    private final Log log;
    private final AmazonS3 s3Client;

    S3Deployer(Log log, AmazonS3 s3Client) {
        this.log = log;
        this.s3Client = s3Client;
    }

    void deploy(String inputDirectory, String bucketName, String outputBasePath, boolean publicRead) {
        if (inputDirectory == null) {
            throw new RuntimeException("must specify inputDirectory parameter in configuration");
        }

        final Path root = new File(inputDirectory).toPath().toAbsolutePath();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = root.relativize(file.toAbsolutePath()).toString();
                    String objectName;
                    if (outputBasePath != null) {
                        objectName = outputBasePath + "/" + relativePath;
                    } else {
                        objectName = relativePath;
                    }
                    log.info("uploading " + file.toFile() + " to " + bucketName + ":" + objectName);
                    PutObjectRequest req = new PutObjectRequest(bucketName, objectName, file.toFile()); //
                    if (publicRead) {
                        AccessControlList acl = new AccessControlList();
                        acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
                        req = req.withAccessControlList(acl);
                    }
                    s3Client.putObject(req);
                    return FileVisitResult.CONTINUE;
                }
            });
            log.info("uploaded files");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
