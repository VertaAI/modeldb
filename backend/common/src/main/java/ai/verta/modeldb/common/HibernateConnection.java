package ai.verta.modeldb.common;

import com.mysql.cj.exceptions.CJCommunicationsException;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.exception.LockAcquisitionException;

import java.net.SocketException;

public abstract class HibernateConnection {
    static final Logger LOGGER = LogManager.getLogger(CommonUtils.class);
    abstract public boolean checkDBConnection();

    abstract public void resetSessionFactory();

    public boolean needToRetry(Exception ex) {
        Throwable communicationsException = findCommunicationsFailedCause(ex);
        if ((communicationsException.getCause() instanceof CommunicationsException)
                || (communicationsException.getCause() instanceof SocketException)
                || (communicationsException.getCause() instanceof CJCommunicationsException)) {
            LOGGER.warn(communicationsException.getMessage());
            LOGGER.warn(
                    "Detected communication exception of type {}",
                    communicationsException.getCause().getClass());
            if (checkDBConnection()) {
                LOGGER.info("Resetting session Factory");

                resetSessionFactory();
                LOGGER.info("Resetted session Factory");
            } else {
                LOGGER.warn("DB could not be reached");
            }
            return true;
        } else if ((communicationsException.getCause() instanceof LockAcquisitionException)) {
            LOGGER.warn(communicationsException.getMessage());
            LOGGER.warn("Retrying since could not get lock");
            return true;
        }
        LOGGER.debug(
                "Detected exception of type {}, which is not categorized as retryable",
                ex,
                communicationsException);
        return false;
    }

    public Throwable findCommunicationsFailedCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null
                && !(rootCause.getCause() instanceof CJCommunicationsException
                || rootCause.getCause() instanceof CommunicationsException
                || rootCause.getCause() instanceof SocketException
                || rootCause.getCause() instanceof LockAcquisitionException)) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    public abstract Session openSession();
}
