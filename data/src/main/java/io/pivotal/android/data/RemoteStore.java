/*
 * Copyright (C) 2014 Pivotal Software, Inc. All rights reserved.
 */
package io.pivotal.android.data;

import android.content.Context;
import android.os.AsyncTask;

public abstract class RemoteStore<T> implements DataStore<T> {

    private final RemoteClient mClient;
    private final ObserverHandler<T> mHandler;

    public RemoteStore(final Context context) {
        this(new ObserverHandler<T>(), new RemoteClient.Default(context));
    }

    public RemoteStore(final ObserverHandler<T> handler, final RemoteClient client) {
        mHandler = handler;
        mClient = client;
    }

    protected RemoteClient getClient() {
        return mClient;
    }

    protected ObserverHandler<T> getHandler() {
        return mHandler;
    }

    @Override
    public void execute(final Request<T> request, final Listener<T> listener) {
        new AsyncTask<Void, Void, Response<T>>() {

            @Override
            protected Response<T> doInBackground(final Void... params) {
                return RemoteStore.this.execute(request);
            }

            @Override
            protected void onPostExecute(final Response<T> resp) {
                if (listener != null) {
                    listener.onResponse(resp);
                }
            }
        }.execute();
    }

    @Override
    public boolean addObserver(final Observer<T> observer) {
        return mHandler.addObserver(observer);
    }

    @Override
    public boolean removeObserver(final Observer<T> observer) {
        return mHandler.removeObserver(observer);
    }
}
