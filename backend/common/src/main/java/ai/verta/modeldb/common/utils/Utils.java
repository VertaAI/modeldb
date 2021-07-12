package ai.verta.modeldb.common.utils;

import com.amazonaws.AmazonServiceException;
import org.apache.logging.log4j.Logger;

public class Utils {

    private Utils() {}

    public static boolean isEnvSet(String envVar) {
        String envVarVal = System.getenv(envVar);
        return envVarVal != null && !envVarVal.isEmpty();
    }

    public static void logAmazonServiceExceptionErrorCodes(Logger LOGGER, AmazonServiceException e) {
        LOGGER.info("Amazon Service Status Code: " + e.getStatusCode());
        LOGGER.info("Amazon Service Error Code: " + e.getErrorCode());
        LOGGER.info("Amazon Service Error Type: " + e.getErrorType());
        LOGGER.info("Amazon Service Error Message: " + e.getErrorMessage());
    }
}
