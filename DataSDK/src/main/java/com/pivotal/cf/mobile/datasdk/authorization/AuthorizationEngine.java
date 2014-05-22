package com.pivotal.cf.mobile.datasdk.authorization;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.pivotal.cf.mobile.common.util.Logger;
import com.pivotal.cf.mobile.datasdk.DataParameters;
import com.pivotal.cf.mobile.datasdk.activity.BaseAuthorizationActivity;
import com.pivotal.cf.mobile.datasdk.prefs.AuthorizationPreferencesProvider;

public class AuthorizationEngine extends AbstractAuthorizationClient {

    // TODO - the state token should be randomly generated, but persisted until the end of the flow
    private static final String STATE_TOKEN = "BLORG";

    public AuthorizationEngine(Context context, AuthorizationPreferencesProvider authorizationPreferencesProvider) {
        super(context, authorizationPreferencesProvider);
    }

    /**
     * Starts the authorization process.
     *
     * @param activity   an already-running activity to use as the base of the authorization process.  This activity
     *                   *MUST* have an intent filter in the AndroidManifest.xml file that captures the redirect URL
     *                   sent by the server.  e.g.:
     *                   <intent-filter>
     *                   <action android:name="android.intent.action.VIEW" />
     *                   <category android:name="android.intent.category.DEFAULT" />
     *                   <category android:name="android.intent.category.BROWSABLE" />
     *                   <data android:scheme="YOUR.REDIRECT_URL.SCHEME" />
     *                   <data android:host="YOUR.REDIRECT.URL.HOST_NAME" />
     *                   <data android:pathPrefix="YOUR.REDIRECT.URL.PATH />
     *                   </intent-filter>
     * @param parameters Parameters object defining the client identification and API endpoints used by
     */
    // TODO - needs a callback to report authorization success/failure.
    public void obtainAuthorization(Activity activity, DataParameters parameters) {
        verifyAuthorizationArguments(activity, parameters);
        saveAuthorizationParameters(parameters);
        startAuthorization(activity, parameters);
    }

    private void verifyAuthorizationArguments(Activity activity, DataParameters parameters) {
        if (activity == null) {
            throw new IllegalArgumentException("activity may not be null");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("parameters may not be null");
        }
        if (parameters.getClientId() == null) {
            throw new IllegalArgumentException("parameters.clientId may not be null");
        }
        if (parameters.getClientSecret() == null) {
            throw new IllegalArgumentException("parameters.clientSecret may not be null");
        }
        if (parameters.getAuthorizationUrl() == null) {
            throw new IllegalArgumentException("parameters.authorizationUrl may not be null");
        }
        if (parameters.getTokenUrl() == null) {
            throw new IllegalArgumentException("parameters.tokenUrl may not be null");
        }
        if (parameters.getRedirectUrl() == null) {
            throw new IllegalArgumentException("parameters.redirectUrl may not be null");
        }
    }

    private void saveAuthorizationParameters(DataParameters parameters) {
        authorizationPreferencesProvider.setClientId(parameters.getClientId());
        authorizationPreferencesProvider.setClientSecret(parameters.getClientSecret());
        authorizationPreferencesProvider.setAuthorizationUrl(parameters.getAuthorizationUrl());
        authorizationPreferencesProvider.setTokenUrl(parameters.getTokenUrl());
        authorizationPreferencesProvider.setRedirectUrl(parameters.getRedirectUrl());
    }

    private void startAuthorization(Activity activity, DataParameters parameters) {
        final AuthorizationCodeFlow flow = getFlow(); // TODO - handle null flow
        final AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
        authorizationUrl.setRedirectUri(parameters.getRedirectUrl().toString());
        authorizationUrl.setState(STATE_TOKEN);
        final String url = authorizationUrl.build();
        Logger.fd("Loading authorization request URL to identify server in external browser: '%s'.", url);
        final Uri uri = Uri.parse(url);
        final Intent i = new Intent(Intent.ACTION_VIEW, uri);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        activity.startActivity(i); // Launches external browser to do complete authentication
    }

    /**
     * Re-entry point to the authorization engine after the user authorizes the application and the
     * server sends back an authorization code.  Calling this method will make the call to the identity
     * server to receive the access token (which is required before calling any protected APIs).
     * This method will fail if it has been called before obtainAuthorization.
     *
     * @param activity          an already-running activity to use as the base of the authorization process.  This activity
     *                          *MUST* have an intent filter in the `AndroidManifest.xml` file that captures the redirect URL
     *                          sent by the server.  Note that the `AuthorizationEngine` will hold a reference to this activity
     *                          until the access token from the identity server has been received and one of the two callbacks
     *                          in the activity have been made.
     * @param authorizationCode the authorization code received from the server.
     */
    public void authorizationCodeReceived(final BaseAuthorizationActivity activity, final String authorizationCode) {

        Logger.fd("Received authorization code from identity server: '%s'.", authorizationCode);

        // TODO - ensure that an authorization flow is already active
        // TODO - remove the AsyncTask after the thread pool is set up.
        final AuthorizationCodeFlow flow = getFlow(); // TODO - handle null flow

        final AsyncTask<Void, Void, TokenResponse> task = new AsyncTask<Void, Void, TokenResponse>() {

            @Override
            protected TokenResponse doInBackground(Void... params) {
                try {
                    final AuthorizationCodeTokenRequest tokenUrl = flow.newTokenRequest(authorizationCode);
                    tokenUrl.setRedirectUri(authorizationPreferencesProvider.getRedirectUrl().toString());
                    return tokenUrl.execute();
                } catch (Exception e) {
                    Logger.ex("Could not get tokens.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(TokenResponse tokenResponse) {
                if (tokenResponse != null) {
                    Logger.fd("Received access token from identity server: '%s'.", tokenResponse.getAccessToken());
                    Logger.d("Authorization flow complete.");
                    storeTokenResponse(flow, tokenResponse);
                    // TODO - report success to callback
                    activity.onAuthorizationComplete();
                } else {
                    Logger.e("Got null token response.");
                    // TODO - report failure to callback - provide a better error message
                    activity.onAuthorizationFailed("Got null token response.");
                }
            }

        };
        task.execute((Void) null);
    }
}
