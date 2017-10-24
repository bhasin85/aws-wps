package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.util.EmailService;
import au.org.aodn.aws.util.JobFileUtil;
import au.org.aodn.aws.wps.status.*;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.SubmitJobRequest;
import com.amazonaws.services.batch.model.SubmitJobResult;
import net.opengis.wps.v_1_0_0.DataInputsType;
import net.opengis.wps.v_1_0_0.DataType;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.InputType;
import net.opengis.wps.v_1_0_0.OutputDefinitionType;
import net.opengis.wps.v_1_0_0.ResponseFormType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import static au.org.aodn.aws.wps.status.WpsConfig.*;

public class ExecuteOperation implements Operation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteOperation.class);


    private final Execute executeRequest;

    public ExecuteOperation(Execute executeRequest) {
        this.executeRequest = executeRequest;
    }


    @Override
    public String execute() {

        //  Config items:
        //      queue names
        //      job name
        //      AWS region
        //      status filename
        //      status location
        String statusS3BucketName = WpsConfig.getConfig(STATUS_S3_BUCKET_CONFIG_KEY);
        String statusFileName = WpsConfig.getConfig(STATUS_S3_FILENAME_CONFIG_KEY);
        String requestFileName = WpsConfig.getConfig(REQUEST_S3_FILENAME_CONFIG_KEY);
        String jobName = WpsConfig.getConfig(AWS_BATCH_JOB_NAME_CONFIG_KEY);
        String jobQueueName = WpsConfig.getConfig(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY);
        String awsRegion = WpsConfig.getConfig(AWS_REGION_CONFIG_KEY);
        String wpsEndpointUrl = WpsConfig.getConfig(WPS_ENDPOINT_URL_CONFIG_KEY);

        LOGGER.info("statusS3BucketName: " + statusS3BucketName);
        LOGGER.info("statusFileName: " + statusFileName);
        LOGGER.info("jobName: " + jobName);
        LOGGER.info("jobQueueName: " + jobQueueName);
        LOGGER.info("awsRegion: " + awsRegion);

        String processIdentifier = executeRequest.getIdentifier().getValue();  // code spaces not supported for the moment
        JobMapper jobMapper = new JobMapper(processIdentifier);
        String jobDefinitionName = jobMapper.getJobDefinitionName();

        Map<String, String> parameterMap = getJobParameters();

        LOGGER.info("Submitting job request...");
        SubmitJobRequest submitJobRequest = new SubmitJobRequest();

        //  TODO: at this point we will need to invoke the correct AWS batch processing job for the function that is specified in the Execute Operation
        //  This will probably involve selecting the appropriate queue, jobName (mainly for display in AWS console) & job definition based on the processIdentifier.
        submitJobRequest.setJobQueue(jobQueueName);  //TODO: config/jobqueue selection
        submitJobRequest.setJobName(jobName);
        submitJobRequest.setJobDefinition(jobDefinitionName);  //TODO: either map to correct job def or set vcpus/memory required appropriately
        submitJobRequest.setParameters(parameterMap);

        AWSBatchClientBuilder builder = AWSBatchClientBuilder.standard();
        builder.setRegion(awsRegion);

        AWSBatch client = builder.build();
        SubmitJobResult result = client.submitJob(submitJobRequest);

        String jobId = result.getJobId();

        LOGGER.info("Job submitted.  Job ID : " + jobId);
        LOGGER.info("Writing job request to S3");
        S3JobFileUpdater s3JobFileUpdater = new S3JobFileUpdater(statusS3BucketName, statusFileName, requestFileName);
        try {
            s3JobFileUpdater.writeRequest(JobFileUtil.createXmlDocument(executeRequest), jobId);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }

        String statusDocument;
        ExecuteStatusBuilder statusBuilder = new ExecuteStatusBuilder(wpsEndpointUrl, jobId, statusS3BucketName, statusFileName);

        try {
            statusDocument = statusBuilder.createResponseDocument(EnumStatus.ACCEPTED, null, null, null);
            s3JobFileUpdater.updateStatus(statusDocument, jobId);

            EmailService emailService = new EmailService();
            String callbackParams = parameterMap.get("callbackParams");
            String email = callbackParams.substring(callbackParams.indexOf("=") + 1);
            emailService.sendRegisteredJobEmail(email, jobId);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            //  Form failed status document
            statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, "Failed to create status file : " + e.getMessage(), "StatusFileError", null);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, e.getMessage(), "EmailError", null);
        }

        return statusDocument;
    }

    private Map<String, String> getJobParameters() {
        Map<String, String> parameters = new LinkedHashMap<>();

        DataInputsType dataInputs = executeRequest.getDataInputs();
        ResponseFormType responseForm = executeRequest.getResponseForm();

        if (dataInputs == null && responseForm == null) {
            return parameters;
        }

        if (dataInputs != null) {
            for (InputType inputType : dataInputs.getInput()) {
                if (inputType.getReference() != null) {
                    throw new UnsupportedOperationException("Input by reference not supported");
                }

                String identifier = inputType.getIdentifier().getValue();  // codespaces not supported for the moment
                DataType data = inputType.getData();
                String literalValue = data.getLiteralData().getValue();   //uom and datatype not supported for the moment
                parameters.put(identifier, literalValue);
            }
        }

        if (responseForm != null) {
            //  Pass the requested output format to the AWS batch aggregator
            OutputDefinitionType outputDefinition = responseForm.getRawDataOutput();
            if(outputDefinition == null) {
                //TODO: what if there are multiple outputs with different mime types
                outputDefinition = responseForm.getResponseDocument().getOutput().get(0);
            }

            String outputMimeType = outputDefinition.getMimeType();
            parameters.put(outputDefinition.getIdentifier().getValue(), outputMimeType);
        }

        return parameters;
    }
}
