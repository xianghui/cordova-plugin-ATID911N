package com.atid.app.mybarcode;

import com.atid.lib.dev.ATScanManager;
import com.atid.lib.dev.ATScanner;
import com.atid.lib.dev.barcode.type.BarcodeType;
import com.atid.lib.dev.barcode.type.EventType;
import com.atid.lib.dev.event.BarcodeEventListener;
import com.atid.lib.system.device.type.BarcodeModuleType;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle; 
import android.os.PowerManager;
import android.os.Vibrator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Context;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Barcode extends CordovaPlugin implements BarcodeEventListener {

private static final String TAG = "MainActivity"; 

private ATScanner mScanner;
private PowerManager.WakeLock mWakeLock = null;
private SoundPool mSoundPool;
private int mBeepSuccess;
private int mBeepFail;
private Vibrator mVibrator;
private CallbackContext keyup_callback = null;
private CallbackContext keydown_callback = null;
private CallbackContext getDecode_callback = null;
private View currentView = null;

ScanResult mScanResult;

//Context context=this.cordova.getActivity().getApplicationContext();

@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
    result.setKeepCallback(true);

    if (action.equals("echo")) {
        String message = args.getString(0);
        this.echo(message, callbackContext);
        return true;
    }
    else if (action.equals("deinitalize_scanner")){
        this.deinitalize();
        return true;
    }
    else if (action.equals("wakeup_scanner")){
        Log.d(TAG, "+- wakeup scanner");
        
        if(mScanner != null)
            ATScanManager.wakeUp();
        return true;
    }
    else if (action.equals("sleep_scanner")){
        Log.d(TAG, "+- sleep scanner");
        if(mScanner != null) {
            ATScanManager.sleep();
        }
        return true;
    }
    else if (action.equals("scanner_startDecode")){
        Log.d(TAG, "++Start Decode");
        mScanResult = null;
        this.mScanner.startDecode();
        Log.d(TAG, "--Start Decode");
        
        return true;
    }
    else if (action.equals("scanner_stopDecode")){
        Log.d(TAG, "++Stop Decode");
        this.mScanner.stopDecode();
        Log.d(TAG, "--Stop Decode");
        
        return true;
    }
    else if (action.equals("scanner_isDecoding")){
        Log.d(TAG, "++Is Decoding");
        callbackContext.success("" + this.mScanner.isDecoding());
        Log.d(TAG, "--Is Decoding");
        
        return true;
    }
    // TODO: replace with  android -> javascript async call instead of javascript->android sync call
    else if (action.equals("scanner_getDecodeCallback")){
        Log.d(TAG, "Start Decode");
        if (mScanResult != null)
        {
            // TODO: return json instead of string
            callbackContext.success("" + mScanResult.scanResultType + 
                " : " + mScanResult.scanResultBarcode);
            mScanResult = null;
        }
        else
            callbackContext.error("Could not get decode information");
        
        return true;
    }
    else if(action.equalsIgnoreCase("register_keyDown")){
            this.keydown_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("register_keyUp")){
            this.keyup_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("register_decode")){
            this.getDecode_callback = callbackContext;
            return true;
    }
    return false;
}

@Override
public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    if ((this.mScanner = ATScanManager.getInstance( BarcodeModuleType.AT2D4710M_1)) == null) {
            Log.e(TAG, "Failure to find scanning device. Aborting...");
            return;
    }
    
    // Initialize Sound Pool
    //this.mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    //this.mBeepSuccess = this.mSoundPool.load(context, R.raw.success, 1);
    //this.mBeepFail = this.mSoundPool.load(context, R.raw.fail, 1);
    // Initialize Vibrator
    /*this.mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);*/
    this.mScanner.setEventListener(this);
    this.currentView = webView.getView();

    Log.i(TAG, "Scanning device initialized");
}


private void echo(String message, CallbackContext callbackContext) {
    if (message != null && message.length() > 0) {
        callbackContext.success(message);
    } else {
        callbackContext.error("Expected one non-empty string argument.");
    }
}

private void deinitalize(){
    Log.d(TAG, "+++ onDeinitalize");
        
    // Deinitalize Scanner Instance
    ATScanManager.finish();

    Log.d(TAG, "--- onDeinitalize");
}

@Override
public void onDecodeEvent(BarcodeType type, String barcode) {

    Log.d(TAG, "onDecodeEvent(" + type + ", [" + barcode
            + "])");

    mScanResult = new ScanResult();
    mScanResult.scanResultType = type;
    mScanResult.scanResultBarcode = barcode;


    
    if(type != BarcodeType.NoRead){
        //int position = this.adapterBarcode.addItem(type, barcode);
        //this.lstBarcodeList.setSelection(position);
        String str = "{\'type\': \'" + type + "\' , \'barcode\': \'" + barcode + "\' }";
        PluginResult result;
        try {
            result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
        } catch(JSONException e){
            e.printStackTrace();
            result = new PluginResult(PluginResult.Status.ERROR, "Error in constructing json for success decode callback");
        }
        result.setKeepCallback(true);
        this.getDecode_callback.sendPluginResult(result);
        //this.getDecode_callback = null;
        return;
         //result.setKeepCallback(false);
        //beep(true);
    }else{
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Barcode not found");
        result.setKeepCallback(true);
        this.getDecode_callback.sendPluginResult(result);
        //this.getDecode_callback = null;
         //result.setKeepCallback(false);
        //beep(false);
        return;
    }

    
}

@Override
public void onStateChanged(EventType state) {
    
    Log.d(TAG, "EventType : " + state.toString());
}

/*
// Beep & Vibrate
private void beep(boolean isSuccess) {
    Log.d(TAG, "@@@@ DEBUG. Play beep....!!!!");
    try{
        if(isSuccess){
            this.mSoundPool.play(mBeepSuccess, 1, 1, 0, 0, 1);
            this.mVibrator.vibrate(100);
        }else{
            this.mSoundPool.play(mBeepFail, 1, 1, 0, 0, 1);
        }
    }catch(Exception ex){
    }
}
*/

private class ScanResult {
   public CallbackContext scanResultCallback; 
   public BarcodeType scanResultType;
   public String scanResultBarcode;



}

}

