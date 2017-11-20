package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.util.AWSBatchUtil;
import au.org.aodn.aws.util.JobFileUtil;
import au.org.aodn.aws.util.S3Utils;
import au.org.aodn.aws.util.Utils;
import au.org.aodn.aws.wps.status.JobStatusFormatEnum;
import au.org.aodn.aws.wps.JobStatusRequest;
import au.org.aodn.aws.wps.JobStatusRequestParameterParser;
import au.org.aodn.aws.wps.JobStatusResponse;
import au.org.aodn.aws.wps.status.QueuePosition;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.JobDetail;
import com.amazonaws.services.batch.model.JobSummary;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.StatusType;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static au.org.aodn.aws.wps.status.WpsConfig.GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.QUEUE_VIEW_HTML_TEMPLATE_S3_BUCKET_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.QUEUE_VIEW_HTML_TEMPLATE_S3_KEY_CONFIG_KEY;


public class JobStatusServiceRequestHandler implements RequestHandler<JobStatusRequest, JobStatusResponse> {

    private static final JobStatusFormatEnum DEFAULT_FORMAT = JobStatusFormatEnum.XML;

    private static final String ACCEPTED_STATUS_DESCRIPTION = "Job accepted";
    private static final String FAILED_STATUS_DESCRIPTION = "Job failed";
    private static final String STARTED_STATUS_DESCRIPTION = "Job processing started";
    private static final String SUCCEEDED_STATUS_DESCRIPTION = "Download ready";
    private static final String PAUSED_STATUS_DESCRIPTION = "Job processing paused";
    private static final String UNKNOWN_STATUS_DESCRIPTION = "Job status unknown";

    private Logger LOGGER = LoggerFactory.getLogger(JobStatusServiceRequestHandler.class);

    private String statusFilename = WpsConfig.getConfig(WpsConfig.STATUS_S3_FILENAME_CONFIG_KEY);
    private String jobFileS3KeyPrefix = WpsConfig.getConfig(WpsConfig.AWS_BATCH_JOB_S3_KEY_PREFIX);
    private String statusS3Bucket = WpsConfig.getConfig(WpsConfig.STATUS_S3_BUCKET_CONFIG_KEY);
    private String configS3Bucket = WpsConfig.getConfig(WpsConfig.STATUS_SERVICE_CONFIG_S3_BUCKET_CONFIG_KEY);
    private String statusXslS3Key = WpsConfig.getConfig(WpsConfig.STATUS_HTML_XSL_S3_KEY_CONFIG_KEY);
    private String requestFilename = WpsConfig.getConfig(WpsConfig.REQUEST_S3_FILENAME_CONFIG_KEY);


    @Override
    public JobStatusResponse handleRequest(JobStatusRequest request, Context context) {

        JobStatusResponse response;
        JobStatusResponse.ResponseBuilder responseBuilder = new JobStatusResponse.ResponseBuilder();
        String responseBody = null;

        JobStatusRequestParameterParser parameterParser = new JobStatusRequestParameterParser(request);
        String jobId = parameterParser.getJobId();
        String format = parameterParser.getFormat();

        LOGGER.info("Parameters passed: JOBID [" + jobId + "], FORMAT [" + format + "]");


        //  Determine the format to send the response in
        JobStatusFormatEnum requestedStatusFormat;
        if (format != null) {
            try {
                requestedStatusFormat = JobStatusFormatEnum.valueOf(format.toUpperCase());
                LOGGER.info("Valid job status format requested : " + requestedStatusFormat.name());
            } catch (IllegalArgumentException iae) {
                String jobStatusValuesString = "";

                JobStatusFormatEnum[] validJobStatuses = JobStatusFormatEnum.values();
                for (int index = 0; index <= validJobStatuses.length - 1; index++) {
                    jobStatusValuesString += validJobStatuses[index].name();
                    if (index + 1 <= validJobStatuses.length - 1) {
                        jobStatusValuesString += ", ";
                    }
                }
                LOGGER.error("UNKNOWN job status format requested [" + format + "].  Supported values : [" + jobStatusValuesString + "].  Defaulting to [" + DEFAULT_FORMAT.name() + "]");
                requestedStatusFormat = DEFAULT_FORMAT;
            }
        } else {
            LOGGER.info("No format parameter passed.  Defaulting to [" + DEFAULT_FORMAT.name() + "]");
            requestedStatusFormat = DEFAULT_FORMAT;
        }



        ExecuteResponse executeResponse = null;
        int httpStatus;
        String statusDescription = null;

        if (requestedStatusFormat.equals(JobStatusFormatEnum.QUEUE)) {
            LOGGER.info("Queue contents requested.");

            AWSBatch batchClient = AWSBatchClientBuilder.defaultClient();
            String queueName = WpsConfig.getConfig(WpsConfig.AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY);

            try {
                responseBody = generateQueueViewHTML(batchClient, queueName);
                httpStatus = HttpStatus.SC_OK;
            }
            catch(IOException | TemplateException ex)
            {
                String errorMessage = "Problem loading queue HTML template: " + ex.getMessage();
                //  Bad stuff - blow up!
                LOGGER.error(errorMessage, ex);
                responseBody = errorMessage;
                httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            }

        } else {
            //  Read the status file for the jobId passed (if it exists)
            try {

                String s3Key = jobFileS3KeyPrefix + jobId + "/" + statusFilename;

                //  Check for the existence of the status document
                AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
                boolean statusExists = s3Client.doesObjectExist(statusS3Bucket, s3Key);


                LOGGER.info("Status file exists for jobId [" + jobId + "]? " + statusExists);

                //  If the status file exists and the job is in an 'waiting' state (we have accepted the job but processing
                //  has not yet commenced) we will attempt to work out the queue position of the job and add that to
                //  the status information we send back to the caller.  If the job is being processed or processing has
                //  completed (successful or failed), then we will return the information contained in the status file unaltered.
                if (statusExists) {

                    String statusXMLString = S3Utils.readS3ObjectAsString(statusS3Bucket, s3Key);

                    //  Read the status document
                    executeResponse = JobFileUtil.unmarshallExecuteResponse(statusXMLString);

                    LOGGER.info("Unmarshalled XML for jobId [" + jobId + "]");


                    StatusType currentStatus = executeResponse.getStatus();
                    //  Get a friendly description of the status
                    statusDescription = getStatusDescription(currentStatus);

                    /**  PARKED UNTIL WE CAN RELIABLY DETERMINE THE QUEUE POSITION OF
                     *   THE JOB USING THE BATCH API.
                     *   CURRENTLY THE AWS QUEUES SEEM TO BE NON-FIFO : SO IMPOSSIBLE TO
                     *   DETERMINE THE ORDER IN WHICH QUEUED JOBS WILL EXECUTE!!
                     *
                     //  If the ExecuteResponse indicates that the job has been accepted but not
                     //  started, completed or failed - then we will update the position indicator.


                     if(isJobWaiting(currentStatus)) {
                     AWSBatch batchClient = AWSBatchClientBuilder.defaultClient();

                     LOGGER.info("Updating XML with progress description for jobId [" + jobId + "]");

                     //  Perform a queue position lookup + insert the position information into the XML
                     //  All we have to do is setProcessAccepted to a string that includes some queue
                     //  position information.
                     String jobProgressDescription = getProgressDescription(batchClient, jobId);

                     LOGGER.info("Progress description: " + jobProgressDescription);

                     if(jobProgressDescription != null) {
                     currentStatus.setProcessAccepted(currentStatus.getProcessAccepted() + " " + jobProgressDescription);
                     executeResponse.setStatus(currentStatus);
                     }

                     responseBody = JobFileUtil.createXmlDocument(executeResponse);
                     } else {
                     //  Return unaltered status XML
                     responseBody = statusXMLString;
                     }
                     **/
                    responseBody = statusXMLString;

                    httpStatus = HttpStatus.SC_OK;
                } else {

                    statusDescription = "Unknown status, error retrieving status [Job ID not found]";
                    LOGGER.info(statusDescription);
                    //  Status document was not found in the S3 bucket
                    httpStatus = HttpStatus.SC_OK;
                    //  Create an empty response object
                    executeResponse = new ExecuteResponse();
                }
            } catch (Exception ex) {
                statusDescription = "Exception retrieving status of job [" + jobId + "]: " + ex.getMessage();
                //  TODO: FORM AN EXCEPTION REPORT?
                //  Bad stuff happened
                LOGGER.error(statusDescription, ex);
                executeResponse = new ExecuteResponse();
                httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            }

            //  If requested output format is HTML - perform a transform on the XML
            if (requestedStatusFormat.equals(JobStatusFormatEnum.HTML) || requestedStatusFormat.equals(JobStatusFormatEnum.ADMIN)) {
                LOGGER.info("HTML output format requested.  Running transform.");
                boolean adminInfoRequested = false;
                if (requestedStatusFormat.equals(JobStatusFormatEnum.ADMIN)) {
                    adminInfoRequested = true;
                }
                responseBody = generateStatusHTML(executeResponse, statusDescription, jobId, adminInfoRequested);
            }
        }

        //  Build the response
        responseBuilder.header("Content-Type", requestedStatusFormat.mimeType());
        responseBuilder.body(responseBody);
        responseBuilder.statusCode(httpStatus);
        responseBuilder.isBase64Encoded(false);
        response = responseBuilder.build();

        return response;
    }


    /**
     * Get a text description of the progress of the job.
     * Currently this description indicates the position of the AWS batch
     * job in the processing queue in the form
     * 'Queue position <POSITION_OF_THE_JOB> of <TOTAL_NUMBER_OF_JOBS_QUEUED>'
     *
     * @param jobId
     * @param batchClient
     * @return
     */
    private String getProgressDescription(AWSBatch batchClient, String jobId) {

        String description = null;
        JobDetail jobDetail = AWSBatchUtil.getJobDetail(batchClient, jobId);

        if (jobDetail != null && jobDetail.getJobId() != null) {

            //  Determine the queue that the job has been assigned to
            String queueName = jobDetail.getJobQueue();
            LOGGER.info("Queue name for jobId [" + jobDetail.getJobId() + "] = " + queueName);

            if (queueName != null) {
                //  Determine the position of the job in the queue
                QueuePosition queuePosition = AWSBatchUtil.getQueuePosition(batchClient, jobDetail);

                if (queuePosition != null) {
                    description = "Queue position " + queuePosition.getPosition() + " of " + queuePosition.getNumberInQueue();
                }
            }
        }

        return description;
    }



    private String generateStatusHTML(ExecuteResponse xmlStatus, String statusDescription, String jobId, boolean includeRequestDetails) {
        // Create Transformer
        TransformerFactory tf = TransformerFactory.newInstance();
        String statusXslString;

        try {
            //  Read XSL from S3
            statusXslString = S3Utils.readS3ObjectAsString(configS3Bucket, statusXslS3Key);
            StringInputStream statusXslInputStream = new StringInputStream(statusXslString);
            StreamSource statusXsltSource = new StreamSource(statusXslInputStream);

            Transformer statusFileTransformer = tf.newTransformer(statusXsltSource);

            //  If we can't determine the submission timestamp pass a -1.
            //  The javascript generated by the XSLT will recognise this as an
            // unknown timestamp.
            long unixTimestampSeconds = -1;

            //  Generate request summary for admin format (if requested)
            try {
                //  Use the request.xml we write to S3 on accepting a job to determine the
                //  submission time of the job
                String requestFileS3Key = jobFileS3KeyPrefix + jobId + "/" + requestFilename;
                LOGGER.info("Request file bucket [" + statusS3Bucket + "], Key [" + requestFileS3Key + "]");
                S3Object requestS3Object = S3Utils.getS3Object(statusS3Bucket, requestFileS3Key);

                if (requestS3Object != null) {
                    long lastModifiedTimestamp = requestS3Object.getObjectMetadata().getLastModified().getTime();
                    unixTimestampSeconds = lastModifiedTimestamp / 1000;
                    LOGGER.info("Request xml file timestamp = " + unixTimestampSeconds);

                    if(includeRequestDetails) {
                        //  Generate request summary details
                        LOGGER.info("Generating request summary HTML for request [" + jobId + "]");
                        String requestSummary = getRequestSummary(requestS3Object);
                        if(requestSummary != null) {
                            statusFileTransformer.setParameter("requestXML", "" + requestSummary);
                        }
                    }
                }

            } catch(Exception ex) {
                LOGGER.error("Unable to determine submission time for job [" + jobId + "]: " + ex.getMessage(), ex);
            }

            //  Pass the job ID & status description
            statusFileTransformer.setParameter("jobid", jobId);
            statusFileTransformer.setParameter("statusDescription", statusDescription);
            //  Pass the unix timestamp to the XSLT - which will render the date in the
            //  locale of the browser.  Passed as seconds since epoch.
            statusFileTransformer.setParameter("submittedTime", "" + unixTimestampSeconds);

            statusFileTransformer.setParameter("bootstrapCssLocation", WpsConfig.getBootstrapCssS3ExternalURL());
            statusFileTransformer.setParameter("aodnCssLocation", WpsConfig.getAodnCssS3ExternalURL());
            statusFileTransformer.setParameter("aodnLogoLocation", WpsConfig.getAodnLogoS3ExternalURL());

            // Source
            JAXBContext jc = JAXBContext.newInstance(ExecuteResponse.class);
            JAXBSource source = new JAXBSource(jc, xmlStatus);

            // Transform
            StringWriter htmlWriter = new StringWriter();
            statusFileTransformer.transform(source, new StreamResult(htmlWriter));

            return htmlWriter.toString();

        } catch (JAXBException jex) {
            LOGGER.error("Unable to generate JAXB context : " + jex.getMessage(), jex);
        } catch (TransformerException tex) {
            LOGGER.error("Unable to generate JAXB context : " + tex.getMessage(), tex);
        } catch (UnsupportedEncodingException uex) {
            LOGGER.error("Unable to generate JAXB context : " + uex.getMessage(), uex);
        } catch (IOException ioex) {
            LOGGER.error("Unable to read status XSL file from S3. Bucket [" + configS3Bucket + "], Key [" + statusXslS3Key + "]: " + ioex.getMessage(), ioex);
        }

        return null;
    }


    private boolean isJobWaiting(StatusType currentStatus) {
        //  WPS status will be ProcessAccepted from the time the job is submitted & when it is
        //  picked up for processing.
        if (currentStatus.isSetProcessAccepted() &&
                (!currentStatus.isSetProcessFailed() && !currentStatus.isSetProcessStarted() && !currentStatus.isSetProcessSucceeded())) {
            return true;
        }
        return false;
    }


    private String getStatusDescription(StatusType currentStatus) {

        if(currentStatus != null) {
            if (currentStatus.isSetProcessAccepted()) {
                return ACCEPTED_STATUS_DESCRIPTION;
            } else if (currentStatus.isSetProcessStarted()) {
                return STARTED_STATUS_DESCRIPTION;
            } else if (currentStatus.isSetProcessSucceeded()) {
                return SUCCEEDED_STATUS_DESCRIPTION;
            } else if (currentStatus.isSetProcessFailed()) {
                return FAILED_STATUS_DESCRIPTION;
            } else if (currentStatus.isSetProcessPaused()) {
                return PAUSED_STATUS_DESCRIPTION;
            }
        }
        return UNKNOWN_STATUS_DESCRIPTION;
    }


    private String getRequestSummary(S3Object requestFileObject) {

        //  Currently just returns the content of the request file.
        //  Can be expanded to provide a richer HTML summary of the request for
        //  admin purposes (possible including links to logs etc).
        try {

            if (requestFileObject != null) {

                S3ObjectInputStream requestInputStream = requestFileObject.getObjectContent();
                StringBuilder requestStringBuilder = new StringBuilder();
                int chunkSizeBytes = 1024;
                byte[] inBytes = new byte[chunkSizeBytes];
                int bytesRead;
                while((bytesRead = requestInputStream.read(inBytes)) > 0) {
                    String inChunk = new String(inBytes, 0, bytesRead -1);
                    requestStringBuilder.append(inChunk);
                }

                return requestStringBuilder.toString();
            } else {
                LOGGER.error("Request S3 object null.");
            }

        } catch (IOException ioex) {
            LOGGER.error("Unable to read request file. Bucket [" + configS3Bucket + "]: " + ioex.getMessage(), ioex);
        }

        return null;
    }


    private String generateQueueViewHTML(AWSBatch batchClient, String queueName) throws IOException, TemplateException {
        List<JobSummary> waitingJobSummaries = AWSBatchUtil.getWaitingJobs(batchClient, queueName);
        List<JobSummary> runningJobSummaries = AWSBatchUtil.getRunningJobs(batchClient, queueName);
        List<JobSummary> completedJobSummaries = AWSBatchUtil.getCompletedJobs(batchClient, queueName);


        List<JobDetail> waitingJobDetails = null;
        LOGGER.info("Waiting jobs: " + waitingJobSummaries.size());
        if(waitingJobSummaries.size() > 0) {

            ArrayList<String> jobIds = new ArrayList<>();
            for (JobSummary summary : waitingJobSummaries) {
                jobIds.add(summary.getJobId());
            }

            waitingJobDetails = AWSBatchUtil.getJobDetails(batchClient, jobIds);
        }

        List<JobDetail> runningJobDetails = null;
        LOGGER.info("Running jobs: " + runningJobSummaries.size());
        if(runningJobSummaries.size() > 0) {

            ArrayList<String> jobIds = new ArrayList<>();
            for (JobSummary summary : runningJobSummaries) {
                jobIds.add(summary.getJobId());
            }

            runningJobDetails = AWSBatchUtil.getJobDetails(batchClient, jobIds);
        }


        List<JobDetail> completedJobDetails = null;
        LOGGER.info("Completed jobs: " + completedJobSummaries.size());
        if(completedJobSummaries.size() > 0) {

            ArrayList<String> jobIds = new ArrayList<>();
            for (JobSummary summary : completedJobSummaries) {
                jobIds.add(summary.getJobId());
            }

            completedJobDetails = AWSBatchUtil.getJobDetails(batchClient, jobIds);
        }


        //  Invoke freemarker template
        String templateS3Bucket = WpsConfig.getConfig(QUEUE_VIEW_HTML_TEMPLATE_S3_BUCKET_CONFIG_KEY);
        String templateS3Key = WpsConfig.getConfig(QUEUE_VIEW_HTML_TEMPLATE_S3_KEY_CONFIG_KEY);

        S3ObjectInputStream contentStream = S3Utils.getS3ObjectStream(templateS3Bucket, templateS3Key);

        //  read file to String
        String templateString = Utils.inputStreamToString(contentStream);

        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate("QueueViewHtmlTemplate", templateString);

        Configuration config = new Configuration();
        config.setClassForTemplateLoading(JobStatusServiceRequestHandler.class, "");
        config.setObjectWrapper(new DefaultObjectWrapper());
        config.setTemplateLoader(stringLoader);
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        Map<String, Object> params = new HashMap<String, Object>();

        params.put("bootstrapCssLocation", WpsConfig.getBootstrapCssS3ExternalURL());
        params.put("aodnCssLocation", WpsConfig.getAodnCssS3ExternalURL());
        params.put("aodnLogoLocation", WpsConfig.getAodnLogoS3ExternalURL());
        params.put("queueName", queueName);

        if (waitingJobDetails != null) {
            params.put("queuedJobsList", waitingJobDetails);
        }

        if (runningJobDetails != null) {
            params.put("runningJobsList", runningJobDetails);
        }

        if (completedJobDetails != null) {
            params.put("completedJobsList", completedJobDetails);
        }

        Template template = config.getTemplate("QueueViewHtmlTemplate");

        LOGGER.info("Got template [QueueViewHtmlTemplate]");
        StringWriter out = new StringWriter();

        template.process(params, out);

        LOGGER.info("Ran template.");

        return out.toString();
    }
}
