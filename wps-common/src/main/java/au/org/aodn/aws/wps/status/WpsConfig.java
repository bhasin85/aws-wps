package au.org.aodn.aws.wps.status;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Properties;

public class WpsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WpsConfig.class);

    //  Configuration key names
    public static final String AWS_BATCH_JOB_NAME_CONFIG_KEY = "AWS_BATCH_JOB_NAME";
    public static final String AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY = "AWS_BATCH_JOB_QUEUE_NAME";
    public static final String AWS_REGION_CONFIG_KEY = "AWS_REGION";
    public static final String AWS_REGION_SES_CONFIG_KEY = "AWS_REGION_SES";
    public static final String STATUS_S3_BUCKET_CONFIG_KEY = "STATUS_S3_BUCKET";
    public static final String STATUS_S3_FILENAME_CONFIG_KEY = "STATUS_S3_FILENAME";
    public static final String OUTPUT_S3_BUCKET_CONFIG_KEY = "OUTPUT_S3_BUCKET";
    public static final String OUTPUT_S3_FILENAME_CONFIG_KEY = "OUTPUT_S3_FILENAME";

    public static final String AWS_BATCH_JOB_ID_CONFIG_KEY = "AWS_BATCH_JOB_ID";
    public static final String AWS_BATCH_CE_NAME_CONFIG_KEY = "AWS_BATCH_CE_NAME";
    public static final String AWS_BATCH_JQ_NAME_CONFIG_KEY = "AWS_BATCH_JQ_NAME";

    public static final String GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY = "GET_CAPABILITIES_TEMPLATE_S3_BUCKET";
    public static final String GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY = "GET_CAPABILITIES_TEMPLATE_S3_KEY";
    public static final String DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY = "DESCRIBE_PROCESS_S3_BUCKET";
    public static final String DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY = "DESCRIBE_PROCESS_S3_KEY_PREFIX";
    public static final String GEOSERVER_WPS_ENDPOINT_URL_CONFIG_KEY = "GEOSERVER_WPS_ENDPOINT_URL";
    public static final String GEOSERVER_WPS_ENDPOINT_TEMPLATE_KEY = "geoserverWPSEndpointURL";

    public static final String AGGREGATOR_CONFIG_S3_BUCKET_CONFIG_KEY = "AGGREGATOR_CONFIG_S3_BUCKET";
    public static final String AGGREGATOR_TEMPLATE_FILE_S3_KEY_CONFIG_KEY = "AGGREGATOR_TEMPLATE_FILE_S3_KEY";
    public static final String DOWNLOAD_CONFIG_S3_KEY_CONFIG_KEY = "DOWNLOAD_CONFIG_S3_KEY";

    public static final String DOWNLOAD_CONNECT_TIMEOUT_CONFIG_KEY = "DOWNLOAD_CONNECT_TIMEOUT";
    public static final String DOWNLOAD_READ_TIMEOUT_CONFIG_KEY = "DOWNLOAD_READ_TIMEOUT";

    public static final String SITE_ACRONYM = "siteAcronym";
    public static final String EMAIL_SIGNATURE = "emailSignature";
    public static final String CONTACT_EMAIL = "contactEmail";
    public static final String FROM_EMAIL = "fromEmail";
    public static final String EMAIL_FOOTER = "emailFooter";
    private static final String EMAIL_TEMPLATES_LOCATION_KEY = "emailTemplatesLocation";
    private static final String COMPLETED_JOB_EMAIL_SUBJECT_KEY = "jobCompleteEmailSubject";
    private static final String COMPLETED_JOB_EMAIL_KEY = "jobCompleteEmail";
    private static final String FAILED_JOB_EMAIL_SUBJECT_KEY = "jobFailedEmailSubject";
    private static final String FAILED_JOB_EMAIL_KEY = "jobFailedEmail";

    public static final String CONFIG_LOCATION_KEY = "CONFIG_LOCATION";

    public static final String S3_BASE_URL = "https://s3.amazonaws.com/";
    public static final String APPLICATION_PROPERTIES = "application.properties";
    private static Properties properties = null;

    private static Properties getProperties() {
        if (properties == null) {
            // Load properties from config
            properties = new Properties();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream resourceStream = loader.getResourceAsStream(APPLICATION_PROPERTIES);
            try {
                properties.load(resourceStream);
            } catch (IOException e) {
                LOGGER.error("Unable to load application properties. Error:", e);
            }

            LOGGER.info("Environment Variables");
            for (String key : System.getenv().keySet()) {
                LOGGER.info(String.format("%s = %s", key, System.getenv(key)));
            }

            // Load properties from environment variables
            setProperty(properties, AWS_BATCH_JOB_NAME_CONFIG_KEY);
            setProperty(properties, AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY);
            setProperty(properties, AWS_REGION_CONFIG_KEY);
            setProperty(properties, AWS_REGION_SES_CONFIG_KEY);
            setProperty(properties, STATUS_S3_BUCKET_CONFIG_KEY);
            setProperty(properties, STATUS_S3_FILENAME_CONFIG_KEY);
            setProperty(properties, OUTPUT_S3_BUCKET_CONFIG_KEY);
            setProperty(properties, OUTPUT_S3_FILENAME_CONFIG_KEY);
            setProperty(properties, AWS_BATCH_JOB_ID_CONFIG_KEY);
            setProperty(properties, AWS_BATCH_CE_NAME_CONFIG_KEY);
            setProperty(properties, AWS_BATCH_JQ_NAME_CONFIG_KEY);

            setProperty(properties, GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY);
            setProperty(properties, GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY);
            setProperty(properties, DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY);
            setProperty(properties, DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY);
            setProperty(properties, GEOSERVER_WPS_ENDPOINT_URL_CONFIG_KEY);
            setProperty(properties, AGGREGATOR_CONFIG_S3_BUCKET_CONFIG_KEY);
            setProperty(properties, AGGREGATOR_TEMPLATE_FILE_S3_KEY_CONFIG_KEY);
            setProperty(properties, DOWNLOAD_CONFIG_S3_KEY_CONFIG_KEY);
        }

        return properties;
    }

    private static void setProperty(Properties properties, String property) {
        String propertyValue = System.getenv(property);
        if (propertyValue != null) {
            properties.put(property, propertyValue);
        }
    }

    public static String getConfig(String configName) {
        return getProperties().getProperty(configName);
    }

    public static String getCompletedJobEmailSubjectTemplate() {
        return String.format("%s/%s", getConfig(EMAIL_TEMPLATES_LOCATION_KEY), getConfig(COMPLETED_JOB_EMAIL_SUBJECT_KEY));
    }

    public static String getCompletedJobEmailTemplate() {
        return String.format("%s/%s", getConfig(EMAIL_TEMPLATES_LOCATION_KEY), getConfig(COMPLETED_JOB_EMAIL_KEY));
    }

    public static String getFailedJobEmailSubjectTemplate() {
        return String.format("%s/%s", getConfig(EMAIL_TEMPLATES_LOCATION_KEY), getConfig(FAILED_JOB_EMAIL_SUBJECT_KEY));
    }

    public static String getFailedJobEmailTemplate() {
        return String.format("%s/%s", getConfig(EMAIL_TEMPLATES_LOCATION_KEY), getConfig(FAILED_JOB_EMAIL_KEY));
    }

    public static String getS3ExternalURL(String s3Bucket, String s3Key) {
        return String.format("%s%s/%s", S3_BASE_URL, s3Bucket, s3Key);
    }

    /**
     * Decrypt the value of a named environment variable.
     *
     * @param keyName
     * @return Decrypted value of the named environment variable.
     */
    private String getEncryptedEnvironmentVariable(String keyName) {
        return decryptKey(System.getenv(keyName));
    }


    /**
     * Decrypt an encrypted environment variable value.
     *
     * @param keyValue
     * @return Decrypted key value.
     */
    private String decryptKey(String keyValue) {
        if (keyValue != null) {
            byte[] encryptedKey = Base64.decode(keyValue);
            AWSKMS client = AWSKMSClientBuilder.defaultClient();
            DecryptRequest request = new DecryptRequest()
                    .withCiphertextBlob(ByteBuffer.wrap(encryptedKey));
            ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
            return new String(plainTextKey.array(), Charset.forName("UTF-8"));
        }
        return null;
    }
}
