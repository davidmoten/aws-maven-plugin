package com.github.davidmoten.aws.maven;

import java.io.File;

public final class Deployer {

	public void deploy(File artifact) {
		//list buckets owned by user
		//if one bucket starts with com.github.davidmoten.aws.maven then use that
		//else create a new one ending with 12 chars from UUID
		//S3 Object name will be artifact filename plus date time (yyyMMddhhmmss)
		//upload artifact to S3 object in bucket
		//call update-application-version with S3 object address
	}
	
}
