/*
 * Copyright (C) 2014 Pivotal Software, Inc. All rights reserved.
 */
package io.pivotal.android.data;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public interface RemoteClient {


    public String get(String url, boolean force) throws Exception;

    public String put(String url, byte[] entity, boolean force) throws Exception;

    public String delete(String url, boolean force) throws Exception;


    public static class Default implements RemoteClient {

        public static final class Timeouts {
            public static final int CONNECTION = 4000;
            public static final int SOCKET = 10000;
        }

        public static final class Headers {
            public static final String AUTHORIZATION = "Authorization";
            public static final String IF_MATCH = "If-Match";
            public static final String IF_NONE_MATCH = "If-None-Match";
            public static final String ETAG = "Etag";
            public static final String USER_AGENT = "User-Agent";
        }

        private final EtagStore mEtagStore;
        private final Context mContext;

        public Default(final Context context) {
            mEtagStore = new EtagStore(context);
            mContext = context;
        }

        public Default(final Context context, final EtagStore store) {
            mEtagStore = store;
            mContext = context;
        }

        @Override
        public String get(final String url, final boolean force) throws Exception {
            final HttpGet request = new HttpGet(url);
            return execute(request, force);
        }

        @Override
        public String put(final String url, final byte[] entity, final boolean force) throws Exception {
            final HttpPut request = new HttpPut(url);
            request.setEntity(new ByteArrayEntity(entity));

            final String result = execute(request, force);
            return TextUtils.isEmpty(result) ? new String(entity) : result;
        }

        @Override
        public String delete(final String url, final boolean force) throws Exception {
            final HttpDelete request = new HttpDelete(url);
            return execute(request, force);
        }

        protected String execute(final HttpUriRequest request, final boolean force) throws Exception {
            final String url = request.getURI().toString();
            final HttpClient httpClient = getHttpClient();

            addHeaders(request, force);

            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() == 401) {
                Logger.v("Response 401 (invalidating token).");
                invalidateAccessToken();

                Logger.v("Response 401 (requesting new token).");
                addAuthHeader(request);

                Logger.v("Response 401 (retrying).");
                response = httpClient.execute(request);
            }

            return handleResponse(response, url);
        }

        protected void addHeaders(final HttpUriRequest request, final boolean force) throws Exception {
            final String url = request.getURI().toString();

            Logger.v("Request Url: " + url);

            addAuthHeader(request);

            addUserAgentHeader(request);

            if (!force) {
                addEtagHeader(request, url);
            } else {
                Logger.e("Request Header - No Etag. Request Forced.");
            }
        }


        // ========================================================


        protected String provideAccessToken() {
            final TokenProvider provider = TokenProviderFactory.obtainTokenProvider();
            if (provider != null) {
                return provider.provideAccessToken(mContext);
            } else {
                return null;
            }
        }

        protected void invalidateAccessToken() {
            final TokenProvider provider = TokenProviderFactory.obtainTokenProvider();
            if (provider != null) {
                provider.invalidateAccessToken(mContext);
            }
        }

        protected void addAuthHeader(final HttpUriRequest request) {
            final String accessToken = provideAccessToken();
            if (accessToken != null) {
                Logger.v("Request Header - " + Headers.AUTHORIZATION + ": Bearer " + accessToken);
                request.addHeader(Headers.AUTHORIZATION, "Bearer " + accessToken);
            } else {
                Logger.e("Request Header - No access token found.");
                throw new IllegalStateException("Could not retrieve access token.");
            }
        }

        protected void addEtagHeader(final HttpUriRequest request, final String url) {
            if (Pivotal.areEtagsEnabled()) {

                final String etag = mEtagStore.get(url);

                if (!TextUtils.isEmpty(etag)) {
                    if (request instanceof HttpGet) {
                        request.addHeader(Headers.IF_NONE_MATCH, etag);
                        Logger.v("Request Header - " + Headers.IF_NONE_MATCH + ": " + etag);
                    } else {
                        request.addHeader(Headers.IF_MATCH, etag);
                        Logger.v("Request Header - " + Headers.IF_MATCH + ": " + etag);
                    }
                } else {
                    if (request instanceof HttpGet) {
                        request.addHeader(Headers.IF_MATCH, "*");
                        Logger.v("Request Header - " + Headers.IF_MATCH + ": *");
                    } else {
                        request.addHeader(Headers.IF_NONE_MATCH, "*");
                        Logger.v("Request Header - " + Headers.IF_NONE_MATCH + ": *");
                    }
                }
            } else {
                Logger.e("Request Header - Etags Disabled.");
            }
        }

        protected void addUserAgentHeader(final HttpUriRequest request) {
            final String sdkVersion = String.format("PCFData/%s;", BuildConfig.SDK_VERSION);
            final String androidVersion = String.format("Android Version %s (Build %s)", Build.VERSION.RELEASE, Build.ID);
            request.addHeader(Headers.USER_AGENT, sdkVersion + " " + androidVersion);
        }


        // ========================================================


        protected HttpClient getHttpClient() throws Exception {
            final HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, Timeouts.CONNECTION);
            HttpConnectionParams.setSoTimeout(params, Timeouts.SOCKET);

            final DefaultHttpClient client = new DefaultHttpClient(params);

            final SSLSocketFactory socketFactory = getSocketFactory();
            if (socketFactory != null) {
                final Scheme scheme = new Scheme("https", socketFactory, 443);
                client.getConnectionManager().getSchemeRegistry().register(scheme);
            }

            return client;
        }

        protected SSLSocketFactory getSocketFactory() throws Exception {
            if (Pivotal.trustAllSslCertificates()) {
                return new TrustAllSSLSocketFactory(null);

            } else if (Pivotal.getPinnedSslCertificateNames().size() > 0) {
                return new SSLSocketFactory(getKeyStore());

            } else {
                return null;
            }
        }

        protected KeyStore getKeyStore() throws Exception {
            final String defaultType = KeyStore.getDefaultType();
            final KeyStore keyStore = KeyStore.getInstance(defaultType);
            keyStore.load(null, null);

            loadCertificates(keyStore);

            return keyStore;
        }

        protected void loadCertificates(final KeyStore keyStore) throws Exception {
            final CertificateFactory certificateFactory = getCertificateFactory();

            final List<String> certificateNames = Pivotal.getPinnedSslCertificateNames();
            for (final String certificateName : certificateNames) {

                loadCertificate(keyStore, certificateFactory, certificateName);
            }
        }

        protected CertificateFactory getCertificateFactory() throws Exception {
            return CertificateFactory.getInstance("X.509");
        }

        protected void loadCertificate(final KeyStore keyStore, final CertificateFactory certificateFactory, final String certificateName) throws Exception {
            final InputStream inputStream = mContext.getAssets().open(certificateName);
            try {
                final Certificate certificate = certificateFactory.generateCertificate(inputStream);
                keyStore.setCertificateEntry(certificateName, certificate);
            } catch (final Exception e) {
                Logger.ex(e);
            } finally {
                inputStream.close();
            }
        }

        protected String handleResponse(final HttpResponse response, final String url) throws Exception {
            final StatusLine statusLine = response.getStatusLine();

            Logger.v("Response Status: " + statusLine);

            final int statusCode = statusLine.getStatusCode();
            final String reasonPhrase = statusLine.getReasonPhrase();

            if (statusCode < 200 || statusCode > 299) {
                if (statusCode == 404 && Pivotal.areEtagsEnabled()) {
                    mEtagStore.put(url, "");
                }

                throw new DataHttpException(statusCode, reasonPhrase);
            }

            if (Pivotal.areEtagsEnabled()) {
                final Header header = response.getFirstHeader(Headers.ETAG);
                final String etag = header != null ? header.getValue() : "";

                Logger.v("Response Header - " + Headers.ETAG + ": " + etag + ", url: " + url);

                mEtagStore.put(url, etag);
            }

            return getResponseBody(response);
        }

        protected String getResponseBody(final HttpResponse response) throws IOException {
            final InputStream inputStream = response.getEntity().getContent();
            final String result = StreamUtils.consumeAndClose(inputStream);

            Logger.v("Response Body: " + result);

            return result;
        }
    }

    public static class TrustAllSSLSocketFactory extends SSLSocketFactory {
        private final SSLContext mSslContext = SSLContext.getInstance("TLS");

        private final TrustManager mTrustManager = new X509TrustManager() {
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            }

            public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        public TrustAllSSLSocketFactory(final KeyStore keyStore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(keyStore);

            mSslContext.init(null, new TrustManager[]{mTrustManager}, null);
        }

        @Override
        public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose) throws IOException {
            return mSslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return mSslContext.getSocketFactory().createSocket();
        }
    }
}