/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by iphoroid team
 */

package org.klab.iphoroid.widget.support;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


/**
 * DownloadTask.
 *
 * @author <a href="mailto:kodama-t@klab.jp">Takuya KODAMA</a> (kodamta-t)
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 */
public abstract class DownloadTask<Param, Result> extends AsyncTask<Param, Void, Result> {

    /** default progress is dialog */
    private ProgressDialog progressDialog;
    /** 2011.7.27 added by jun */
    private Context mContext;

    /**
     * @param context for dialog
     */
    public DownloadTask(Context context, String message) {
        this.progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(true);
        mContext = context;
    }

    /** default progress is dialog */
    public void showProgress() {
        progressDialog.show();
    }

    /** default progress is dialog */
    public void dismissProgress() {
try {
        progressDialog.dismiss();
} catch (IllegalArgumentException e) {
 Log.e("DownloadTask", e.getMessage(), e);
}
    }

    /** */
    public abstract Result download(Param ... params) throws Exception;

    /**
     * @param result null when error occurs
     */
    public abstract void setResult(Result result);

    @Override
    protected void onPreExecute() {
        showProgress();
    }

    @Override
    protected final Result doInBackground(Param... params) {

    	// 2011.7.27 fixed by jun
    	// synchronized化でメモリ不足対策
    	if (mContext!= null) {
	    	synchronized (mContext) {
		        try {
		            Result items = download(params);
		            return items;
		        } catch (Exception e) {
		            Log.e("DownloadTask", e.getMessage(), e);
		            return null;
		        }
			}
    	} else {
	        try {
	            Result items = download(params);
	            return items;
	        } catch (Exception e) {
	            Log.e("DownloadTask", e.getMessage(), e);
	            return null;
	        }
    	}
    }

    /*
     * @return null when error occurs
     */
    @Override
    protected void onPostExecute(Result result) {
        dismissProgress();
        setResult(result);
    }

    /** 各々でエラーハンドリングしなくていいように */
    public static abstract class DefaultDawnloadTask<Param, Result> extends DownloadTask<Param, Result> {

        private Context context;

        /**
         * @param context for dialog
         */
        public DefaultDawnloadTask(Context context, String message) {
            super(context, message);
            this.context = context;
        }

        @Override
        protected final void onPostExecute(Result result) {
            dismissProgress();
            if (result != null) {
                setResult(result);
            } else {
                new AlertDialog.Builder(context).setTitle("Error").setMessage("Downloading").show();
            }
        }
    }
}
