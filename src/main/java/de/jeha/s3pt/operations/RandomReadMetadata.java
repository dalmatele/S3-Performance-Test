package de.jeha.s3pt.operations;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import de.jeha.s3pt.OperationResult;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author jenshadlich@googlemail.com
 */
public class RandomReadMetadata extends AbstractOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RandomReadMetadata.class);
    private final static Random GENERATOR = new Random();

    private final AmazonS3 s3Client;
    private final String bucketName;
    private final int n;

    public RandomReadMetadata(AmazonS3 s3Client, String bucketName, int n) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.n = n;
    }

    @Override
    public OperationResult call() {
        LOG.info("Random read: n={}", n);

        LOG.info("Collect objects for test");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int filesRead = 0;
        Map<Integer, String> files = new HashMap<>();
        // this should give a thousand objects
        // TODO: make it explicit and support pagination
        for (S3ObjectSummary objectSummary : s3Client.listObjects(bucketName).getObjectSummaries()) {
            files.put(filesRead, objectSummary.getKey());
            filesRead++;
        }
        stopWatch.stop();

        LOG.info("Time = {} ms", stopWatch.getTime());

        LOG.info("Objects read for test: {}", filesRead);

        for (int i = 0; i < n; i++) {
            final String randomKey = (filesRead == 1)
                    ? files.get(0)
                    : files.get(GENERATOR.nextInt(files.size() - 1));
            LOG.debug("Read object: {}", randomKey);

            stopWatch = new StopWatch();
            stopWatch.start();

            ObjectMetadata objectMetadata = s3Client.getObjectMetadata(bucketName, randomKey);
            LOG.debug("Object version: {}", objectMetadata.getVersionId());

            stopWatch.stop();

            LOG.debug("Time = {} ms", stopWatch.getTime());
            getStats().addValue(stopWatch.getTime());

            if (i > 0 && i % 1000 == 0) {
                LOG.info("Progress: {} of {}", i, n);
            }
        }

        return new OperationResult(getStats());
    }

}