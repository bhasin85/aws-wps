package au.org.aodn.aws.wps.request;

import net.opengis.wps.v_1_0_0.DataInputsType;
import net.opengis.wps.v_1_0_0.DocumentOutputDefinitionType;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.InputType;

import java.util.List;

import static au.org.aodn.aws.wps.status.WpsConfig.TEST_TRANSACTION_INPUT_IDENTIFIER;

public class ExecuteRequestHelper {
    private final Execute request;

    public ExecuteRequestHelper(Execute request) {
        this.request = request;
    }

    public String getLiteralInputValue(String identifier) {
        for (InputType input: request.getDataInputs().getInput()) {
            if (input.getIdentifier().getValue().equals(identifier)) {
                return input.getData().getLiteralData().getValue();
            }

        }

        return null;
    }

    public boolean hasRequestedOutput(String identifier) {
        for (DocumentOutputDefinitionType output: getResponseFormOutputs()) {
            if (output.getIdentifier().getValue().equals(identifier)) {
                return true;
            }
        }

        return false;
    }

    public String getRequestedMimeType(String identifier) {
        for (DocumentOutputDefinitionType output: getResponseFormOutputs()) {
            if (output.getIdentifier().getValue().equals(identifier)) {
                return output.getMimeType();
            }
        }

        return null;
    }

    private List<DocumentOutputDefinitionType> getResponseFormOutputs() {
        if(request.getResponseForm() != null && request.getResponseForm().getResponseDocument() != null) {
            return request.getResponseForm().getResponseDocument().getOutput();
        }

        return null;
    }

    public String getEmail() {
        String callbackParams = getLiteralInputValue("callbackParams");

        if (callbackParams != null) {
            return callbackParams.substring(callbackParams.indexOf("=") + 1);
        } else {
            return null;
        }
    }


    /**
     * Check to see if this is a test transaction or not.  A test transaction is indicated by the presence of an Input in
     * the DataInputs section of the Execute request whose name is TestMode and whose literal value is 'true'.
     * eg:
     *  <wps:Input>
     *      <ows:Identifier>TestMode</ows:Identifier>
     *      <wps:Data>
     *          <wps:LiteralData>true</wps:LiteralData>
     *      </wps:Data>
     *  </wps:Input>
     *
     * @param executeRequest
     * @return
     */
    public static boolean isTestTransaction(Execute executeRequest) {

        if(executeRequest.getDataInputs() != null && executeRequest.getDataInputs().getInput() != null && executeRequest.getDataInputs().getInput().size() > 0)
        {
            DataInputsType dataInputs = executeRequest.getDataInputs();
            List<InputType> inputs = dataInputs.getInput();

            for(InputType input : inputs) {
                if(input.getIdentifier() != null) {
                    String inputName = input.getIdentifier().getValue();
                    if(inputName.equalsIgnoreCase(TEST_TRANSACTION_INPUT_IDENTIFIER)) {
                        if(input.getData() != null && input.getData().getLiteralData() != null) {
                            String value = input.getData().getLiteralData().getValue();
                            return Boolean.parseBoolean(value);
                        }
                    }
                }
            }
        }

        return false;
    }
}
