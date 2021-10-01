package ru.gang.logdoc.utils;

import javax.net.ssl.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author Denis Danilin | denis@danilin.name
 * 04.07.2015 15:28
 * ☭ sweat and blood
 */
@SuppressWarnings("unused")
public class Httper {
    private final static SSLSocketFactory ignoranceFactory;

    static {
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                    }

                    @Override
                    public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        try {
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            ignoranceFactory = sslContext.getSocketFactory();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<String, String> headers;

    private final boolean useCheckCertificate;
    private final long timeOutMs;

    public Httper(final boolean useCheckCertificate, final long timeOutMs) {
        this.useCheckCertificate = useCheckCertificate;
        this.timeOutMs = timeOutMs;
        headers = new HashMap<>(0);
        headers.put("Content-Type", "application/octet-stream");
    }

    public int execute(final URL url, final Supplier<byte[]> payload) throws Exception {
        final URLConnection urlConnection = url.openConnection();

        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);

        ((HttpURLConnection) urlConnection).setRequestMethod("POST");
        ((HttpURLConnection) urlConnection).setInstanceFollowRedirects(false);
        urlConnection.setUseCaches(false);

        if (timeOutMs > 0) {
            urlConnection.setConnectTimeout((int) timeOutMs);
            urlConnection.setReadTimeout((int) timeOutMs);
        }

        for (final String headerName : headers.keySet())
            urlConnection.setRequestProperty(headerName, headers.get(headerName));

        if (url.getProtocol().equalsIgnoreCase("https") && !useCheckCertificate)
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(ignoranceFactory);

        urlConnection.connect();

        final OutputStream os = urlConnection.getOutputStream();
        os.write(payload.get());
        os.flush();

        try {
            return ((HttpURLConnection) urlConnection).getResponseCode();
        } finally {
            try { os.close(); } catch (final Exception ignore) { }
            try { ((HttpURLConnection) urlConnection).disconnect(); } catch (final Exception ignore) { }
        }
    }
}
