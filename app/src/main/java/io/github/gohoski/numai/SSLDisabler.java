package io.github.gohoski.numai;

/**
 * Created by Gleb on 20.09.2025.
 * Disable certificate check
 */

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class SSLDisabler {

    /**
     * Disables SSL certificate checking for Http(s)URLConnection.
     * Call this once before making any HTTPS connections.
     */
    static void disableSSLCertificateChecking() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // Do nothing
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // Do nothing
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLSv1");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true; // Allow all hostnames
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}