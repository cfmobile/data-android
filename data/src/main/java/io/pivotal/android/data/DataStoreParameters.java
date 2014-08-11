/*
 * Copyright (C) 2014 Pivotal Software, Inc. All rights reserved.
 */
package io.pivotal.android.data;

import io.pivotal.android.data.util.StringUtil;

public class DataStoreParameters {

    private final String clientId;
    private final String clientSecret;
    private final String authorizationUrl;
    private final String redirectUrl;
    private final String dataServicesUrl;

    public DataStoreParameters(String clientId, String clientSecret, String authorizationUrl, String redirectUrl, String dataServicesUrl) {

        if (clientId != null) {
            this.clientId = clientId.trim();
        } else {
            this.clientId = null;
        }

        if (clientSecret != null) {
            this.clientSecret = clientSecret.trim();
        } else {
            this.clientSecret = null;
        }

        if (authorizationUrl != null) {
            this.authorizationUrl = StringUtil.trimTrailingSlashes(authorizationUrl.trim());
        } else {
            this.authorizationUrl = null;
        }

        if (redirectUrl != null) {
            this.redirectUrl = StringUtil.trimTrailingSlashes(redirectUrl.trim());
        } else {
            this.redirectUrl = null;
        }

        if (dataServicesUrl != null) {
            this.dataServicesUrl = StringUtil.trimTrailingSlashes(dataServicesUrl.trim());
        } else {
            this.dataServicesUrl = null;
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getDataServicesUrl() {
        return dataServicesUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DataStoreParameters other = (DataStoreParameters) o;

        if (clientId != null ? !clientId.equals(other.clientId) : other.clientId != null) {
            return false;
        }
        if (clientSecret != null ? !clientSecret.equals(other.clientId) : other.clientSecret != null) {
            return false;
        }
        if (authorizationUrl != null ? !authorizationUrl.equals(other.authorizationUrl) : other.authorizationUrl != null) {
            return false;
        }
        if (redirectUrl != null ? !redirectUrl.equals(other.redirectUrl) : other.redirectUrl != null) {
            return false;
        }
        if (dataServicesUrl != null ? !dataServicesUrl.equals(other.dataServicesUrl) : other.dataServicesUrl != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = clientId != null ? clientId.hashCode() : 0;
        result = 31 * result + (clientSecret != null ? clientSecret.hashCode() : 0);
        result = 31 * result + (authorizationUrl != null ? authorizationUrl.hashCode() : 0);
        result = 31 * result + (redirectUrl != null ? redirectUrl.hashCode() : 0);
        result = 31 * result + (dataServicesUrl != null ? dataServicesUrl.hashCode() : 0);
        return result;
    }
}
