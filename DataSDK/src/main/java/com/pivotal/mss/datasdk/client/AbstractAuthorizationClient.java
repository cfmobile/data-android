package com.pivotal.mss.datasdk.client;

import com.pivotal.mss.datasdk.DataParameters;
import com.pivotal.mss.datasdk.api.ApiProvider;
import com.pivotal.mss.datasdk.api.AuthorizedApiRequest;
import com.pivotal.mss.datasdk.prefs.AuthorizationPreferencesProvider;

public class AbstractAuthorizationClient {

    protected ApiProvider apiProvider;
    protected AuthorizationPreferencesProvider authorizationPreferencesProvider;

    public AbstractAuthorizationClient(ApiProvider apiProvider,
                                       AuthorizationPreferencesProvider authorizationPreferencesProvider) {

        verifyArguments(authorizationPreferencesProvider, apiProvider);
        saveArguments(authorizationPreferencesProvider, apiProvider);
    }

    private void verifyArguments(AuthorizationPreferencesProvider authorizationPreferencesProvider,
                                 ApiProvider apiProvider) {

        if (authorizationPreferencesProvider == null) {
            throw new IllegalArgumentException("authorizationPreferencesProvider may not be null");
        }
        if (apiProvider == null) {
            throw new IllegalArgumentException("httpRequestFactoryProvider may not be null");
        }
    }

    private void saveArguments(AuthorizationPreferencesProvider authorizationPreferencesProvider,
                               ApiProvider apiProvider) {
        this.authorizationPreferencesProvider = authorizationPreferencesProvider;
        this.apiProvider = apiProvider;
    }

    protected void checkIfAuthorizationPreferencesAreSaved() throws AuthorizationException {
        if (authorizationPreferencesProvider.getClientId() == null || authorizationPreferencesProvider.getClientId().isEmpty()) {
            throw new AuthorizationException("parameters.clientId may not be null or empty");
        }
        if (authorizationPreferencesProvider.getClientSecret() == null || authorizationPreferencesProvider.getClientSecret().isEmpty()) {
            throw new AuthorizationException("parameters.clientSecret may not be null or empty");
        }
        if (authorizationPreferencesProvider.getAuthorizationUrl() == null) {
            throw new AuthorizationException("parameters.authorizationUrl may not be null");
        }
        if (authorizationPreferencesProvider.getTokenUrl() == null) {
            throw new AuthorizationException("parameters.tokenUrl may not be null");
        }
        if (authorizationPreferencesProvider.getRedirectUrl() == null || authorizationPreferencesProvider.getRedirectUrl().isEmpty()) {
            throw new AuthorizationException("parameters.redirectUrl may not be null");
        }
        if (authorizationPreferencesProvider.getDataServicesUrl() == null) {
            throw new AuthorizationException("parameters.getDataServicesUrl may not be null");
        }
    }

    // TODO - write Javadocs
    public void setParameters(DataParameters parameters) throws Exception {
        verifyDataParameters(parameters);
        if (!isInitialParameters() && areParametersUpdated(parameters)) {
            final AuthorizedApiRequest request = apiProvider.getAuthorizedApiRequest(authorizationPreferencesProvider);
            request.clearSavedCredentialAsynchronously(null);
        }
        saveDataParameters(parameters);
    }

    private void verifyDataParameters(DataParameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters may not be null");
        }
        if (parameters.getClientId() == null || parameters.getClientId().isEmpty()) {
            throw new IllegalArgumentException("parameters.clientId may not be null or empty");
        }
        if (parameters.getClientSecret() == null || parameters.getClientSecret().isEmpty()) {
            throw new IllegalArgumentException("parameters.clientSecret may not be null or empty");
        }
        if (parameters.getAuthorizationUrl() == null) {
            throw new IllegalArgumentException("parameters.authorizationUrl may not be null");
        }
        if (parameters.getTokenUrl() == null) {
            throw new IllegalArgumentException("parameters.tokenUrl may not be null");
        }
        if (parameters.getRedirectUrl() == null || parameters.getRedirectUrl().isEmpty()) {
            throw new IllegalArgumentException("parameters.redirectUrl may not be null");
        }
        if (parameters.getDataServicesUrl() == null) {
            throw new IllegalArgumentException("parameters.dataServicesUrl may not be null");
        }
    }

    private boolean isInitialParameters() {
        return authorizationPreferencesProvider.getClientId() == null ||
                authorizationPreferencesProvider.getClientSecret() == null ||
                authorizationPreferencesProvider.getAuthorizationUrl() == null ||
                authorizationPreferencesProvider.getTokenUrl() == null ||
                authorizationPreferencesProvider.getRedirectUrl() == null ||
                authorizationPreferencesProvider.getDataServicesUrl() == null;
    }

    private boolean areParametersUpdated(DataParameters parameters) {
        if (!authorizationPreferencesProvider.getClientId().equals(parameters.getClientId())) {
            return true;
        }
        if (!authorizationPreferencesProvider.getClientSecret().equals(parameters.getClientSecret())) {
            return true;
        }
        if (!authorizationPreferencesProvider.getAuthorizationUrl().equals(parameters.getAuthorizationUrl())) {
            return true;
        }
        if (!authorizationPreferencesProvider.getTokenUrl().equals(parameters.getTokenUrl())) {
            return true;
        }
        if (!authorizationPreferencesProvider.getRedirectUrl().equals(parameters.getRedirectUrl())) {
            return true;
        }
        return false;
    }

    private void saveDataParameters(DataParameters parameters) {
        authorizationPreferencesProvider.setClientId(parameters.getClientId());
        authorizationPreferencesProvider.setClientSecret(parameters.getClientSecret());
        authorizationPreferencesProvider.setAuthorizationUrl(parameters.getAuthorizationUrl());
        authorizationPreferencesProvider.setTokenUrl(parameters.getTokenUrl());
        authorizationPreferencesProvider.setRedirectUrl(parameters.getRedirectUrl());
        authorizationPreferencesProvider.setDataServicesUrl(parameters.getDataServicesUrl());
    }
}
