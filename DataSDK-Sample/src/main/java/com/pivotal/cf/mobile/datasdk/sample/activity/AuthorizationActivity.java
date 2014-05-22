package com.pivotal.cf.mobile.datasdk.sample.activity;

import android.content.Intent;
import android.os.Bundle;

import com.pivotal.cf.mobile.datasdk.activity.BaseAuthorizationActivity;
import com.pivotal.cf.mobile.datasdk.sample.R;

public class AuthorizationActivity extends BaseAuthorizationActivity {

/*
    The functionality of this activity is taken care of by its parent class: `BaseAuthorizationActivity`.
    It is okay to be "mostly" empty - except for any code that you want to add to control the flow
    between the activities in your application.

    This activity is opened *after* your user has authenticated themselves with the Pivotal CF Mobile Service
    identity provider server.  It will be launched by the external browser and receive the redirect URL
    transmitted by the identify provider server.

    This activity does not require any user interface if you don't want.  You can simply finish it (after
    `onResume)`

    This activity must be declared in your `AndroidManifest.xml` file.

    It *MUST* have the following intent filter set up to match the _redirect URL_ passed in the
    `DataParameters` object to the `DataSDK.obtainAuthorization` method:

         <intent-filter>
             <action android:name="android.intent.action.VIEW" />
             <category android:name="android.intent.category.DEFAULT" />
             <category android:name="android.intent.category.BROWSABLE" />

             <data android:scheme="YOUR.REDIRECT_URL.SCHEME" />   <-- Fill the scheme in here!
             <data android:host="YOUR.REDIRECT.URL.HOST_NAME" />  <-- Fill the host in here!
             <data android:pathPrefix="YOUR.REDIRECT.URL.PATH />  <-- Fill the path in here!

         </intent-filter>
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If you have not provided a UI for the authorization activity, then you will want to `finish`
        // it here.
    }

    // This callback is called after authorization is successfully completed.
    // You can use this opportunity to launch (or return to) the next activity in the
    // "secured" portion of your application.
    @Override
    public void onAuthorizationComplete() {
        returnToMainActivity();
    }

    // This callback is called after authorization has failed.  You can use this
    // opportunity to launch (or return to) some activity outside the "secured"
    // portion of your application.
    @Override
    public void onAuthorizationFailed(String reason) {
        returnToMainActivity();
    }

    private void returnToMainActivity() {
        // TODO - pass error reason back to `MainActivity`?
        // TODO - don't call `startActivity` on `MainActivity` if it is finished.
        // NOTE: in this case, MainActivity has a `singleInstance` launch mode, so it will
        // always bring us back to the pre-existing instance of MainActivity.
        final Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

}
