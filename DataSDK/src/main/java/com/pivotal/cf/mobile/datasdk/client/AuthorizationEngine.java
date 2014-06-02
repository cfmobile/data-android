package com.pivotal.cf.mobile.datasdk.client;

import android.app.Activity;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.pivotal.cf.mobile.common.util.Logger;
import com.pivotal.cf.mobile.datasdk.activity.BaseAuthorizationActivity;
import com.pivotal.cf.mobile.datasdk.api.ApiProvider;
import com.pivotal.cf.mobile.datasdk.api.AuthorizedApiRequest;
import com.pivotal.cf.mobile.datasdk.prefs.AuthorizationPreferencesProvider;

public class AuthorizationEngine extends AbstractAuthorizationClient {

    public AuthorizationEngine(ApiProvider apiProvider,
                               AuthorizationPreferencesProvider authorizationPreferencesProvider) {

        super(apiProvider, authorizationPreferencesProvider);
    }


    /**
     * Starts the authorization process.
     *  @param activity   an already-running activity to use as the base of the authorization process.  May not be null.
     *                   This activity *MUST* have an intent filter in the AndroidManifest.xml file that captures the
     *                   redirect URL sent by the server.  e.g.:
     *
     *                   <intent-filter>
     *                      <action android:name="android.intent.action.VIEW" />
     *                      <category android:name="android.intent.category.DEFAULT" />
     *                      <category android:name="android.intent.category.BROWSABLE" />
     *                      <data android:scheme="YOUR.REDIRECT_URL.SCHEME" />
     *                      <data android:host="YOUR.REDIRECT.URL.HOST_NAME" />
     *                      <data android:pathPrefix="YOUR.REDIRECT.URL.PATH />
     *                   </intent-filter>
     *
     */
    // TODO - describe thrown exceptions
    public void obtainAuthorization(Activity activity) throws Exception {
        if (activity == null) {
            throw new IllegalArgumentException("activity may not be null");
        }
        checkIfAuthorizationPreferencesAreSaved();
        startAuthorization(activity);
    }


    private void startAuthorization(Activity activity) throws Exception {

        // Launches external browser to do complete authentication
        final AuthorizedApiRequest request = apiProvider.getAuthorizedApiRequest(authorizationPreferencesProvider);
        request.obtainAuthorization(activity);
    }

    /**
     * Re-entry point to the authorization engine after the user authorizes the application and the
     * server sends back an authorization code.  Calling this method will make the call to the identity
     * server to receive the access token (which is required before calling any protected APIs).
     * This method will fail if it has been called before obtainAuthorization.
     *
     * This method assumes that it is called on the main thread.
     *
     * @param activity          an already-running activity to use as the base of the authorization process.  This activity
     *                          *MUST* have an intent filter in the `AndroidManifest.xml` file that captures the redirect URL
     *                          sent by the server.  Note that the `AuthorizationEngine` will hold a reference to this activity
     *                          until the access token from the identity server has been received and one of the two callbacks
     *                          in the activity have been made.  May not be null.
     *
     * @param authorizationCode the authorization code received from the server.
     */
    // TODO - describe thrown exceptions
    public void authorizationCodeReceived(final BaseAuthorizationActivity activity, final String authorizationCode) throws Exception {

        Logger.fd("Received authorization code from identity server: '%s'.", authorizationCode);

        if (activity == null) {
            throw new IllegalArgumentException("activity may not be null");
        }

        checkIfAuthorizationPreferencesAreSaved();

        final AuthorizedApiRequest request = apiProvider.getAuthorizedApiRequest(authorizationPreferencesProvider);

        // If no authorization was returned then clear any saved credentials and return an error
        if (authorizationCode == null || authorizationCode.isEmpty()) {

            request.clearSavedCredentialAsynchronously(new AuthorizedApiRequest.ClearSavedCredentialListener() {

                @Override
                public void onSavedCredentialCleared() {
                    activity.onAuthorizationFailed("no authorization code was returned.");
                }
            });

        } else {

            // TODO - ensure that an authorization flow is already active
            request.getAccessToken(authorizationCode, new AuthorizedApiRequest.AuthorizationListener() {

                @Override
                public void onSuccess(TokenResponse tokenResponse) {
                    activity.onAuthorizationComplete();
                }

                @Override
                public void onAuthorizationDenied() {
                    request.clearSavedCredentialSynchronously();
                    activity.onAuthorizationDenied();
                }

                @Override
                public void onFailure(String reason) {
                    activity.onAuthorizationFailed(reason);
                }
            });
        }
    }

    // TODO - add Javadocs
    public void clearAuthorization() throws Exception {
        checkIfAuthorizationPreferencesAreSaved();
        final AuthorizedApiRequest request = apiProvider.getAuthorizedApiRequest(authorizationPreferencesProvider);
        request.clearSavedCredentialAsynchronously(null);
    }

}
