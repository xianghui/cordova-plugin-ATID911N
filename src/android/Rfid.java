package com.atid.app.myRfid;

import com.atid.lib.dev.ATRfidManager;
import com.atid.lib.dev.ATRfidReader;
import com.atid.lib.dev.event.RfidReaderEventListener;
import com.atid.lib.dev.rfid.type.ActionState;
import com.atid.lib.dev.rfid.type.BankType;
import com.atid.lib.dev.rfid.type.ConnectionState;
import com.atid.lib.dev.rfid.type.ResultCode;
import com.atid.lib.dev.rfid.type.TagType;
import com.atid.lib.dev.rfid.param.RangeValue;
import com.atid.lib.dev.rfid.exception.ATRfidReaderException;


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


public class Rfid extends CordovaPlugin implements RfidReaderEventListener {

private static final String TAG = "RFID Native"; 

protected ATRfidReader mReader;
private PowerManager.WakeLock mWakeLock = null;
private SoundPool mSoundPool;
private int mBeepSuccess;
private int mBeepFail;
private Vibrator mVibrator;

private CallbackContext keyup_callback = null;
private CallbackContext keydown_callback = null;
private CallbackContext onReaderReadTag_callback = null;
private CallbackContext onReaderResult_callback = null;
private CallbackContext onReaderStateChanged_callback = null;
private CallbackContext onReaderActionChanged_callback = null;
private View currentView = null;


//Context context=this.cordova.getActivity().getApplicationContext();

@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
    result.setKeepCallback(true);
    
    // lifecycle functions //
    if (action.equals("initialize")){
        if ((mReader = ATRfidManager.getInstance()) == null) {
            Log.e(TAG, "Failure to initialize RFID device. Aborting...");
            callbackContext.error("Failure to initialize RFID device");
            return true;
        }

        ATRfidManager.wakeUp();
        callbackContext.success("successfully initialized RFID device");
        return true;
    }
    else if (action.equals("deinitalize")){
        this.deinitalize();
        return true;
    }
    else if (action.equals("wakeup")){
        Log.d(TAG, "+- wakeup scanner");

        if(mReader != null)
            ATRfidManager.wakeUp();

        callbackContext.success("Called wakeUp function");
        return true;
    }
    else if (action.equals("sleep")){
        Log.d(TAG, "+- sleep scanner");
        if(mReader != null) {
            ATRfidManager.sleep();
        }

        callbackContext.success("Called sleep function");
        return true;
    }
    else if (action.equals("forceSleep")){
        Log.d(TAG, "+- force sleep scanner");
        if(mReader != null) {
            mReader.disconnect();
        }

        callbackContext.success("Called sleep function");
        return true;
    }
    else if (action.equals("forceWake")){
        Log.d(TAG, "+- force wake scanner");
        if(mReader != null) {
            if(!mReader.connect()) {
                Log.e(TAG, "ERROR. wakeUp() - Failed to connect rfid reader");
                callbackContext.error("ERROR. wakeUp() - Failed to connect rfid reader");
            } else {
                Log.i(TAG, "INFO. wakeUp()");
                callbackContext.success("Called wakeUp function");
            }
        }

        
        return true;
    }
    else if (action.equals("pause_scanner")){
        Log.d(TAG, "+- pause scanner");
        if (mReader != null)
            mReader.removeEventListener(this);
        return true;
    }
    else if (action.equals("resume_scanner")){
        Log.d(TAG, "+- resume scanner");
        if (mReader != null)
            mReader.setEventListener(this);
        return true;
    }

    // getters and setters //
    
    else if (action.equals("getActionState")){
        callbackContext.success((mReader.getAction()) + "");
        return true;
    }
    else if (action.equals("getPowerRange")){
        Log.d(TAG, "++Get Power Range");
        try {
            callbackContext.success(new JSONObject("{\'min\': \'" + mReader.getPowerRange().getMin() + "\' , \'max\' : \'" + mReader.getPowerRange().getMax() + "\' }"));
        } catch (ATRfidReaderException e)
        {
            callbackContext.error("failed to get power range");
        }
        Log.d(TAG, "--Get Power Range");

        return true;
    }
    else if (action.equals("getPower")){
        Log.d(TAG, "++Get Power");
        try {
            callbackContext.success("" + mReader.getPower());
        }catch (ATRfidReaderException e)
        {
            callbackContext.error("failed to get power");
        }
        Log.d(TAG, "--Get Power");

        return true;
    }
    else if (action.equals("getOperationTime")){
        Log.d(TAG, "++Get OperationTime");
        try {
            callbackContext.success("" + mReader.getOperationTime());
        } catch (ATRfidReaderException e)
        {
            callbackContext.error("failed to get operation time");
        }
        Log.d(TAG, "--Get OperationTime");

        return true;
    }
    else if (action.equals("setPower")){
        Log.d(TAG, "++set power level");
        try {
            mReader.setPower(args.getInt(0));
            callbackContext.success("successfully set power level");
        } catch (ATRfidReaderException e) {
            Log.e(TAG, String.format(
                    "ERROR. saveOption() - Failed to set power level [%s]",
                    e.getCode()), e);
            callbackContext.error("failed to set power level");
        }

        Log.d(TAG, "--set power level");

        return true;
    }
    else if (action.equals("setOperationTime")){
        Log.d(TAG, "++set operation time");

        try {
            mReader.setOperationTime(args.getInt(0));
            callbackContext.success("successfully set operation time");
        }catch (ATRfidReaderException e) {
            Log.e(TAG, String.format(
                    "ERROR. saveOption() - Failed to set operation Time [%s]",
                    e.getCode()), e);
            callbackContext.error("failed to operation time");
        }

        Log.d(TAG, "--set operation time");

        return true;
    }
    else if (action.equals("setInventoryTime")){
        Log.d(TAG, "++setInventoryTime");

        try {
            mReader.setInventoryTime(args.getInt(0));
            callbackContext.success("successfully setInventoryTime");
        }catch (ATRfidReaderException e) {
            Log.e(TAG, String.format(
                    "ERROR. saveOption() - Failed to setInventoryTime [%s]",
                    e.getCode()), e);
            callbackContext.error("failed to setInventoryTime");
        }

        Log.d(TAG, "--setInventoryTime");

        return true;
    }
    else if (action.equals("setIdleTime")){
        Log.d(TAG, "++ setIdleTime");

        try {
            mReader.setIdleTime(args.getInt(0));
            callbackContext.success("successfully setIdleTime");
        } catch (ATRfidReaderException e) {
            Log.e(TAG, String.format(
                    "ERROR. saveOption() - Failed to set idle Time [%s]",
                    e.getCode()), e);
            callbackContext.error("failed setIdleTime");
        }

        Log.d(TAG, "-- setIdleTime");

        return true;
    }

    // Reading and Writing //

    else if (action.equals("start_readContinuous"))
    {
        if (!args.getBoolean(0)){
            startAction(TagType.Tag6C, true, callbackContext);
        }
        else{
            final CallbackContext myCallbackContext = callbackContext;
            Log.d(TAG, "Starting read continuous on new thread");
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    startAction(TagType.Tag6C, true, myCallbackContext);
                }
            });
        }
        return true;
    }
    else if (action.equals("start_readSingle"))
    {
        if (!args.getBoolean(0)){
            startAction(TagType.Tag6C, false, callbackContext);
        }
        else{
            final CallbackContext myCallbackContext = callbackContext;
            Log.d(TAG, "Starting read single on new thread");
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    startAction(TagType.Tag6C, false, myCallbackContext);
                }
            });
        }
        
        return true;
    }
    else if (action.equals("start_readMemory"))
    {
        startAction(TagType.Tag6C, args.getJSONObject(0), true, callbackContext);
        return true;
    }
    else if (action.equals("start_writeMemory"))
    {
        startAction(TagType.Tag6C, args.getJSONObject(0), false, callbackContext);
        return true;
    }
    else if (action.equals("stop_read"))
    {
        final CallbackContext myCallbackContext = callbackContext;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                stopAction(myCallbackContext);
            }
        });
        
        return true;
    }
    else if (action.equals("isStopped"))
    {
        final CallbackContext myCallbackContext = callbackContext;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                myCallbackContext.success((mReader.getAction() == ActionState.Stop) + "");
            }
        });
        
        return true;
    }



    // Events //

    else if(action.equalsIgnoreCase("register_keyDown")){
            this.keydown_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("register_keyUp")){
            this.keyup_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("onReaderReadTag")){
            this.onReaderReadTag_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("onReaderResult")){
            this.onReaderResult_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("onReaderStateChanged")){
            this.onReaderStateChanged_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("onReaderActionChanged")){
            this.onReaderActionChanged_callback = callbackContext;
            return true;
    }
    return false;
}

@Override
public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    if ((mReader = ATRfidManager.getInstance()) == null) {
            Log.e(TAG, "Failure to initialize RFID device. Aborting...");
            return;
    }

    ATRfidManager.wakeUp();
    mReader.setEventListener(this);
    
    this.currentView = webView.getView();

    Log.i(TAG, "RFID device initialized");
}

//Added by Lek Hsiang Hui
//to ensure that the all lifecycle is properly handled like in the sample app
//(specifically that the app destroy the reader object)
@Override 
public void onStart(){
    super.onStart();

    Log.i(TAG, "INFO. onStart()");

    ATRfidManager.wakeUp();
}

@Override
public void onStop(){
    Log.i(TAG, "INFO. onStop()");
    ATRfidManager.sleep();

    super.onStop();
}

@Override
public void onResume(boolean multitasking){
    super.onResume(multitasking);

    Log.i(TAG, "INFO. onResume()");

    if (mReader != null)
        mReader.setEventListener(this);
}

@Override
public void onPause(boolean multitasking){
    Log.i(TAG, "INFO. onPause()");

    if (mReader != null)
        mReader.removeEventListener(this);

    super.onPause(multitasking);
}

@Override
public void onDestroy(){
    Log.i(TAG, "INFO. onDestroy()");

    this.deinitalize();

    super.onDestroy();
}
//------------------------------------


private void deinitalize(){
    Log.d(TAG, "+++ onDeinitalize");
        
    ATRfidManager.onDestroy();

    Log.d(TAG, "--- onDeinitalize");
}

protected void startAction(TagType tagType, boolean isContinuous, CallbackContext callbackContext) {

    ResultCode res = null;

    if (isContinuous) {
        // Multi Reading
        switch (tagType) {
        case Tag6C:
            if ((res = mReader.inventory6cTag()) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to start inventory 6C tag [%s]",
                                res));

                 callbackContext.error(String.format(
                                "ERROR. startAction() - Failed to start inventory 6C tag [%s]",
                                res));
                return;
            }
            break;
        case Tag6B:
            if ((res = mReader.inventory6bTag()) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to start inventory 6B tag [%s]",
                                res));
               
                return;
            }
            break;
        }
    } else {
        // Single Reading
        switch (tagType) {
        case Tag6C:
            if ((res = mReader.readEpc6cTag()) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to start read 6C tag [%s]",
                                res));
                callbackContext.error(String.format(
                                "ERROR. startAction() - Failed to start read 6C tag [%s]",
                                res));
                return;
            }
            break;
        case Tag6B:
            if ((res = mReader.readEpc6bTag()) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to start read 6B tag [%s]",
                                res));
                
                return;
            }
            break;
        }
    }
    callbackContext.success(isContinuous ? "successfully reading continuously: " + res : "successfully read single: " + res);
    Log.i(TAG, "INFO. startAction()");
}

protected void startAction(TagType tagType, JSONObject params, boolean isRead, CallbackContext callbackContext) {
    ResultCode res;
    BankType bank;
    int offset;
    int length;
    String data;
    String password;

    // TODO: set up logic for multiple banktype selection
    try {
        bank = params.isNull("bankType") ? BankType.EPC : params.getString("bankType").equalsIgnoreCase("EPC") ? BankType.EPC : null;
        offset = params.isNull("offset") ? 2 :  params.getInt("offset");
        length = params.isNull("length") ? 2 : params.getInt("length");
        password =  params.isNull("password") ? "" : params.getString("password");
        data =  params.isNull("data") ? "" : params.getString("data");
        
    } catch(JSONException e)
    {
        e.printStackTrace();
        callbackContext.error("Failed to read/write to memory, invalid JSON error.");
        return;
    }

    switch (tagType) {
        case Tag6C:
            //bank = getBank();
            if (isRead){
                if ((res = mReader.readMemory6c(bank, offset, length, password)) != ResultCode.NoError) {
                    Log.e(TAG,
                            String.format(
                                    "ERROR. startAction() - Failed to read memory 6C tag [%s]",
                                    res));
                    
                    callbackContext.error(String.format(
                                    "ERROR. startAction() - Failed to read memory 6C tag [%s]",
                                    res));
                    return;
                }

                callbackContext.success("successfully read memory : " + res);
            }
            else {
                if ((res = mReader.writeMemory6c(bank, offset, data, password)) != ResultCode.NoError) {
                    Log.e(TAG,
                            String.format(
                                    "ERROR. startAction() - Failed to write memory 6C tag [%s]",
                                    res));
                    
                    callbackContext.error(String.format(
                                    "ERROR. startAction() - Failed to write memory 6C tag [%s]",
                                    res));
                    return;
                }
                callbackContext.success("successfully wrote to memory : " + res);
            }
            break;
        case Tag6B:
            if ((res = mReader.readMemory6b(offset, length)) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to read memory 6B tag [%s]",
                                res));
                
                return;
            }
            break;
        }
        
        Log.i(TAG, "INFO. startAction()");
}

protected void stopAction(CallbackContext callbackContext) {

    ResultCode res;

    if ((res = mReader.stop()) != ResultCode.NoError) {
        Log.e(TAG, String.format(
                "ERROR. stopAction() - Failed to stop operation [%s]", res));
        callbackContext.error(String.format(
                "ERROR. stopAction() - Failed to stop operation [%s]", res));
        return;
    }

    callbackContext.success("successfully stopped reading operation");

    Log.i(TAG, "INFO. stopAction()");
}


/*
    ####### onReader event functions 
*/

@Override
public void onReaderActionChanged(ATRfidReader reader, ActionState action) {
    Log.i(TAG, String.format("EVENT. onReaderActionchanged(%s)", action));
    if (action == ActionState.Stop) {
      //  adpTags.shutDown();
    } else {
        //adpTags.start();
    }

    if (this.onReaderActionChanged_callback == null)
        return;
    
    try {
        String str = action + "";
        PluginResult result = new PluginResult(PluginResult.Status.OK, str);
        result.setKeepCallback(true);
        this.onReaderActionChanged_callback.sendPluginResult(result);
        return;
    } catch(Exception e)
    {
        e.printStackTrace();
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderActionChanged event");
        result.setKeepCallback(true);
        this.onReaderActionChanged_callback.sendPluginResult(result);
        return;
    }

    //enableWidgets(true);
}

@Override
public void onReaderReadTag(ATRfidReader reader, String tag, float rssi) {

    //adpTags.addItem(tag, rssi);
    //txtCount.setText(String.format("%d", adpTags.getCount()));
    //playSuccess();

    Log.i(TAG,
            String.format("EVENT. onReaderReadTag([%s], %.2f)", tag, rssi));
    if (this.onReaderReadTag_callback == null)
        return;
    
    try {
        String str = "{\'tag\':\'" + tag + "\' , \'rssi\': \'" + rssi + "\' }";
        PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
        result.setKeepCallback(true);
        this.onReaderReadTag_callback.sendPluginResult(result);
        return;
    } catch(Exception e)
    {
        e.printStackTrace();
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderReadTag event");
        result.setKeepCallback(true);
        this.onReaderReadTag_callback.sendPluginResult(result);
        return;
    }
}

@Override
public void onReaderResult(ATRfidReader reader, ResultCode code,
        ActionState action, String epc, String data) {
    Log.i(TAG, String.format("EVENT. onReaderResult(%s, %s, [%s], [%s]",
            code, action, epc, data));

    if (this.onReaderResult_callback == null)
        return;
    
    try {
        String str = String.format("{\'code\': \'%s\', \'action\': \'%s\', \'epc\':\'%s\', \'data\':\'%s\'}", code, action, epc, data);
        PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
        result.setKeepCallback(true);
        this.onReaderResult_callback.sendPluginResult(result);
        return;
    } catch(Exception e)
    {
        e.printStackTrace();
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderResult event");
        result.setKeepCallback(true);
        this.onReaderResult_callback.sendPluginResult(result);
        return;
    }
}

@Override
public void onReaderStateChanged(ATRfidReader reader, ConnectionState state) {
    Log.i(TAG, String.format("EVENT. onReaderStateChanged(%s)", state));
    if (this.onReaderStateChanged_callback == null)
        return;
    
    try {
        String str = state + "";
        PluginResult result = new PluginResult(PluginResult.Status.OK, str);
        result.setKeepCallback(true);
        this.onReaderStateChanged_callback.sendPluginResult(result);
        return;
    } catch(Exception e)
    {
        e.printStackTrace();
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderStateChanged event");
        result.setKeepCallback(true);
        this.onReaderStateChanged_callback.sendPluginResult(result);
        return;
    }
}


}

