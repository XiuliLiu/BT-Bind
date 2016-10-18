//=============================================================================
//
// Copyright 2016 Ximmerse, LTD. All rights reserved.
//
//=============================================================================

package com.ximmerse.io.ble;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;

import com.ximmerse.io.StreamState;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BleAutoConnectManager2{

    protected static final String TAG=BleAutoConnectManager.class.getSimpleName();

    protected static Context sContext;

    protected static BluetoothAdapter sBluetoothAdapter;
    protected static BluetoothLeScanner sBluetoothLeScanner;
    protected static List<ScanFilter> sScanFilters;
    protected static ScanSettings.Builder sScanSettings;
    protected static int sMinRssi=-200;
    protected static boolean sIsScanning=false;
    protected static long sStartScanTime=-1;
    protected static int sScanTimeout=10000;// 10s
    
    protected static int deviceMax = 2;

    public static void setDeviceMax(int max){
    if (deviceMax > 4){deviceMax = 4;}
    else{deviceMax = max;}
    }
    
    protected static ScanCallback sOnScanCallback=new ScanCallback(){
        @Override
        public void onScanResult(int callbackType,ScanResult result){
            switch(callbackType){
                case  ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                    sOnLeScanCallback.onLeScan(result.getDevice(),result.getRssi(),null);
                break;
            }
        }
    };
    
    protected static BluetoothAdapter.LeScanCallback sOnLeScanCallback=new BluetoothAdapter.LeScanCallback(){
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            //
            String sScanDeviceInfo = "";
        	try {
				//Fixed ...
        		Class<?> ScanRecord = Class.forName("android.bluetooth.le.ScanRecord");
				Method parseFromBytes = ScanRecord.getMethod("parseFromBytes", byte[].class);
				ScanRecord s = (ScanRecord)parseFromBytes.invoke(null, (Object)scanRecord);
                //find id
                for (ParcelUuid id:s.getServiceUuids()){
                    Log.e(TAG, "id : "+id.toString());
                    if (id.toString().startsWith("0000f")){
                        sScanDeviceInfo = id.toString();
                    }
                }
				
			} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
            if(sScanTimeout>0&&SystemClock.elapsedRealtime()>=(sStartScanTime+sScanTimeout)){
                Log.d(TAG,"Stop scan because SystemClock.elapsedRealtime()>=(sStartScanTime+sScanTimeout)");
                stopScan();
                return;
            }
            // We don't need to scan all the time,if there is not any request.
            if(sRequestOrders.size()<=0){
                Log.d(TAG,"Stop scan because sRequestOrders.size()<=0");
                stopScan();
                return;
            }
            //
            Log.d(TAG,"Scan Result : {Name=\""+device.getName()+"\", Address="+device.getAddress()+", Rssi="+rssi+"}");
            
            if( rssi>=sMinRssi){
                tryConnectBluetoothDevice(device, sScanDeviceInfo);
            }
        }
    };

    protected static BleStream[] sStreams=new BleStream[4];
    protected static List<Integer> sRequestOrders=new ArrayList<Integer>();

    public static void setContext(Context context){
        if(context==null){
            sContext=context;
            sBluetoothAdapter=null;
        }else if(sContext==null){
            sContext=context;
            //
            IntentFilter filter =new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            sContext.registerReceiver(sBluetoothAdapterReceiver,filter);
            //
            checkBluetoothAdapter();
        }
    }

    public static boolean checkBluetoothAdapter(){
        //
        if(sBluetoothLeScanner==null) sBluetoothLeScanner=sBluetoothAdapter==null||!sBluetoothAdapter.isEnabled()?null:sBluetoothAdapter.getBluetoothLeScanner();
        if(sScanFilters==null) sScanFilters=new ArrayList<ScanFilter>();
        if(sScanSettings==null) sScanSettings=new ScanSettings.Builder().setReportDelay(500);
        //
        if(sBluetoothAdapter==null){
            //
            sBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
            if (!sBluetoothAdapter.isEnabled()) {if(sContext!=null){
                Log.i(TAG, "onClick - BT not enabled yet");
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if(sContext instanceof Activity) {
                    ((Activity)sContext).startActivityForResult(enableIntent, BleStream.REQUEST_ENABLE_BT);
                }else{
                    enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    sContext.startActivity(enableIntent);
                }
            }}
        }
        return sBluetoothAdapter.isEnabled();
    }

    private static BroadcastReceiver sBluetoothAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE, 0);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            startScan();
                        break;
                    }
                break;
            }
        }
    };
    
    public static void setMinRssi(int rssi){
        sMinRssi=rssi;
    }
    
    public static void setScanReportDelay(int delay){
        sScanSettings.setReportDelay(delay);
    }
    
    public static void setScanTimeout(int timeout){
        sScanTimeout=timeout;
    }

    public static void addRequest(BleStream stream,int order){
        //Log.d(TAG,"Info : Stream"+stream+" Order="+order);
        // Check and fix.
        if(!checkBluetoothAdapter()){
            return;
        }
        if(order<0){
            return;
        }
        //
        if(sStreams[order]!=null){if(sStreams[order]!=stream){
            Log.d(TAG,"sStreams[order]!=null@addRequest");
            //return;
        }}
        sStreams[order]=stream;
        // Very important!!!
        if(sRequestOrders.indexOf(order)==-1){
            if(stream!=null&&!stream.isOpen()) {
                sRequestOrders.add(order);
                stream.dispatchStateChangedEvent(StreamState.SCANNING);
                //Log.d(TAG,"Add Device@(order="+order+") to request pool.");
            }else {
                //Log.d(TAG,"Device@(order="+order+") has connected.");
            }
        }
        startScan();
    }

    public static void removeRequest(BleStream stream,int order) {
        if(sStreams[order]==stream){
            //sStreams[order]=null;// Don't erase it,keep it for check in connectBluetoothDevice(int,String);
            // Very important!!!
            sRequestOrders.remove(new Integer(order));
            if(sRequestOrders.size()<=0){
                stopScan();
            }
        }
    }

    public static void startScan(){
        if(!checkBluetoothAdapter()){
            return;
        }
        sStartScanTime=SystemClock.elapsedRealtime();
        if(!sIsScanning){
            sIsScanning=true;
            sBluetoothAdapter.startLeScan(sOnLeScanCallback);
            //sBluetoothLeScanner.startScan(/*sScanFilters*/null,sScanSettings.build(),sOnScanCallback);
        }
    }

    public static void stopScan(){
        if(!checkBluetoothAdapter()){
            return;
        }
        if(sIsScanning){
            sIsScanning=false;
            sBluetoothAdapter.stopLeScan(sOnLeScanCallback);
            //sBluetoothLeScanner.stopScan(sOnScanCallback);
        }
    }
    
    /** Try find an available stream in <b>Request Pool</b> to connect with BluetoothDevice.*/
    public static void tryConnectBluetoothDevice(BluetoothDevice device, String scanInfo){
        //
        String address=device.getAddress();
        BleStream s;
        int i=0,imax=sRequestOrders.size();
        for(;i<imax;++i){s=sStreams[sRequestOrders.get(i)];
            if(s!=null&&address.equals(s.getAddress())){// Open last address.
                if(connectBluetoothDevice(sRequestOrders.get(i),address)) {
                    return;
                }
            }
        }
        //new device
        i=getConnectOrder(scanInfo);
        if(i>=0){
            if(sRequestOrders.indexOf(i)!=-1){
                if(connectBluetoothDevice(i,address)){
                    return;
                }
            }else {
           }
        }
    }

    /** Use <b>order arg</b> and <b>mac address</b> to connect the stream with BluetoothDevice.*/
    public static boolean connectBluetoothDevice(int order,String address){
        BleStream stream=sStreams[order];
        boolean ret=false;
        // We can't lock a BluetoothDevice with the specific stream,so we need to
        // check if there is the other stream in Stream Pool connected with this address.
        int checkOrder=-1;
        for(int i=0,imax=sStreams.length;i<imax;++i){
            if(sStreams[i]!=null&&address.equals(sStreams[i].getAddress())){
                checkOrder=i;
                break;
            }
        }
        if(checkOrder==-1){checkOrder=order;}
        // We need pass two conditions.
        if(stream!=null&&order==checkOrder){
            String streamAddress=stream.getAddress();
            if(stream.isOpen()){// When open
                if(address.equals(streamAddress)){
                    ret=true;
                }else {
                    Log.d(TAG,"Device@(order="+order+") has been connected to "+stream.getAddress()+".");
                    ret=true;
                }
            }else {// When close.
                if(address.equals(streamAddress)){
                    Log.d(TAG,"Device@(order="+order+") trys to connect to "+stream.getAddress()+" again.");
                }else {//if(StringUtil.isNullOrEmpty(streamAddress)){
                    stream.setAddress(address);
                }
                stream.open();
                ret=true;
            }
        }else{
        }
        //
        if(ret) {
            Log.d(TAG,"Device@(order="+checkOrder+") auto connect to "+address);
            // Very important!!!
            sRequestOrders.remove(new Integer(order));
            if(sRequestOrders.size()<=0){
                stopScan();
            }
        }
        //
        return ret;
    }

    // <!-- Need optimization

    public final static int BLE_DEVICE_COBRA_L = 0x00;
    public final static int BLE_DEVICE_COBRA_R = 0x01;
    public final static int BLE_DEVICE_XHWAK = 0x02;
    public final static int BLE_DEVICE_IMU = 0x03;
    public final static int BLE_DEVICE_UNKNOW = -1;

    public static int parseConnectOrder(String name){
        switch(name.toLowerCase()){
            case "xcobra-0":return BLE_DEVICE_COBRA_L;
            case "xcobra-1":return BLE_DEVICE_COBRA_R;
        }
        return BLE_DEVICE_UNKNOW;
    }

    //  f00x   user mode
    //  f10x   calibration
    //  ff0x    connecting mode

    public static int getConnectOrder(String sInfo){

       byte[] info = sInfo.getBytes();
        if (info.length < 8){
            return BLE_DEVICE_UNKNOW;
        }
        Log.e(TAG, "ss "+ info[5]);
        if (info[5] == '0'){
            return  BLE_DEVICE_UNKNOW;
        }

        byte value = (byte) (info[7] - 0x30);
        Log.e(TAG, "value: "+ value);
        if ((value%2)==1){return BLE_DEVICE_COBRA_L;}
        return BLE_DEVICE_COBRA_R;
    }
    

    public static boolean checkStreamConnected(int order){
        if(sStreams[order]!=null){
            return sStreams[order].isOpen();
        }
        return false;
    }
}
