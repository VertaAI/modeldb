package ai.verta.modeldb.common.certificates;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.lang3.StringUtils;

public class TrustManagers {

  public static TrustManager[] trustManagers(final String selfSignedCertPath, String certificateName) {
    if (StringUtils.isNotEmpty(selfSignedCertPath)) {
      try {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        Certificate certificate =
            CertificateFactory.getInstance("X.509")
                .generateCertificate(new FileInputStream(selfSignedCertPath));
        ks.setCertificateEntry(certificateName, certificate);
        tmf.init(ks);
        return tmf.getTrustManagers();
      } catch (Exception e) {
        throw new ModelDBException("Fail to load self-signed certificate", e);
      }
    }
    return new javax.net.ssl.TrustManager[0];
  }

}
