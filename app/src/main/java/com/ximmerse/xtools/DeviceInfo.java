//=============================================================================
//
// Copyright 2016 Ximmerse, LTD. All rights reserved.
//
//=============================================================================

package com.ximmerse.xtools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.ximmerse.io.FileUtility;
import com.ximmerse.io.IStreamListener;
import com.ximmerse.io.IStreamable;
import com.ximmerse.io.IniFile;
import com.ximmerse.io.StreamState;
import com.ximmerse.io.ble.BleAutoConnectManager2;
import com.ximmerse.io.ble.BleStream;
import com.ximmerse.utils.StringUtil;


import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import widget.com.ximmerse.widget.ItemListView;
import widget.com.ximmerse.widget.SimpleAdapter;

public class DeviceInfo implements  ItemListView.OnItemClickListener<DeviceInfo>, SimpleAdapter.OnViewCreatedListener, IStreamListener {

    public static final int SCANNING = 1;
    public static final int BINDED = 2;
    public static final int NONE_BIND = 3;

    public static final  int
         STREAM_UNKNOWN     = 0
        ,STREAM_BLUETOOTH   = 1
        ,STREAM_USB          = 2
    ;

    private static final String TAG = DeviceInfo.class.getSimpleName();

    public static int getStreamType(String streamName){
        switch (streamName.toLowerCase()){
            case "ble":
                return STREAM_BLUETOOTH;
            case "usb":
                return STREAM_USB;
            default:
                return STREAM_UNKNOWN;
        }
    }
    public   void updateInfoView(Activity context, final ItemListView<DeviceInfo> infoView){
        if(infoView!=null){
            Object[] icons=infoView.getItemIcons();
            String[] fields=infoView.getItemFields();
            List<DeviceInfo> list=infoView.getItemList();
            DeviceInfo info;
            List<HashMap<String,Object>> itemUiRes=infoView.getItemUiRes();
            HashMap<String,Object> map;
            //
            int i=0,imax= Math.min(itemUiRes.size(),list.size());
            for(;i<imax;i++){
                map=itemUiRes.get(i);
                info=list.get(i);
                map.put(fields[1], info.getDisplayName());
                if(map!=null&&info!=null) {
                    Log.d(TAG,"devIsCheck:"+info.devIsCheck);
                  if(!info.devIsCheck)
                    {
                        map.put(fields[0], icons[0]);
                        map.put(fields[2], "NONE_BIND");
                        map.put(fields[3], "");
                        map.put(fields[4], "");
                    }
                    else {
                      map.put(fields[0], icons[1]);
                      if (info.getStreamState() == DeviceInfo.NONE_BIND){
                              map.put(fields[2], "NONE_BIND");
                              map.put(fields[3], "");
                              map.put(fields[4], "");
                          }else if(info.getStreamState() == DeviceInfo.BINDED){
                              Log.d(TAG,".deviceName:"+info.deviceName+";address:"+info.address);
                              map.put(fields[2], "BINDED");
                              map.put(fields[3], info.deviceName);
                              map.put(fields[4], info.address);

                          }else if(info.getStreamState() == DeviceInfo.SCANNING){
                              map.put(fields[2], "SCANNING");
                              map.put(fields[3], "");
                              map.put(fields[4], "");
                      }
                  }

                }

            }
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    infoView.setDirty();
                }
            });

        }
    }

    public Activity context;
    public ItemListView<DeviceInfo> parentView;

    public String name;
    public String deviceName;
    public String address;
    public String streamTypeName;
    public int streamType;
    public BleStream stream;
    protected int streamState = DeviceInfo.NONE_BIND;
    private static String cfgPath= Environment.getExternalStorageDirectory().getPath()+ "/Ximmerse/Runtime/common.ini";
    protected static Object sIniHelperLock=new Object();
    protected static IniFile sIniHelper=new IniFile();
    public static boolean loadInI(Activity context){
       String parentPath=Environment.getExternalStorageDirectory().getPath()+ "/Ximmerse/Runtime/";
        File file = new File(parentPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        if(FileUtility.exists(cfgPath)){
            sIniHelper.load(cfgPath);
            saveUsbInFile();
        }


        return  true;
    }
    private   SharedPreferences.Editor editor;
    private   SharedPreferences mSharedPreferences;
    public DeviceInfo(Activity context, String name){
        this.context=context;
        this.name = name;
        synchronized(sIniHelperLock){
            this.deviceName         = sIniHelper.getItem(name,"DeviceName",name);
            this.streamTypeName  = sIniHelper.getItem(name,"StreamType",name);
            this.address      = sIniHelper.getItem(name,"Address",address);
            this.streamType=getStreamType(streamTypeName);
        }
        stream = BleStream.newInstance(context);

        if(!StringUtil.isNullOrEmpty(address)){
            stream.setAddress(address);
            streamState = DeviceInfo.BINDED;
        }
        stream.setOnStreamStateChangedListener(this);
        stream.setOnStreamReadListener(this);
        mSharedPreferences = context.getSharedPreferences("DevIsCheck", Context.MODE_PRIVATE);
        editor = mSharedPreferences.edit();
        devIsCheck=mSharedPreferences.getBoolean(name,true);
    }


    public void updateView(){
        if(context!=null&&parentView!=null) {
            updateInfoView(context,parentView);
        }

        if(mButtonCnn!=null){
            //mButtonCnn.setText(isOpen?"Disconnect":"Connect");
        }
    }
    public void syncInfo(){

    }

    public String getDisplayName(){
        return  name;
    }

    public void showToast(final String msg){
        if(context!=null){
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context,msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void showToast(final int msgResId){
        if(context!=null){
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context,msgResId, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    @Override
    public void onItemClick(ItemListView<DeviceInfo> parent, View view, DeviceInfo item, HashMap<String, Object> uiRes, boolean longClick) {
      //  doConnect(!longClick);
    }


    private  Button mButtonCnn;
    private CheckBox mCheckBox;
    private    boolean  devIsCheck;
    @Override
    public void onViewCreated(int position, View view) {
        Log.d(TAG,"ViewCreated");
        //mTextDeviceName=(TextView)view.findViewById(R.id.textView_DevName);
       // mTextDeviceState=(TextView)view.findViewById(R.id.textView_DevState);
        mButtonCnn=(Button)view.findViewById(R.id.button_Connect0);
        mButtonCnn.setOnClickListener(onButtonCnnClicked);
        mCheckBox= (CheckBox) view.findViewById(R.id.checkbox1);
        mCheckBox.setChecked(devIsCheck);
       mButtonCnn.setEnabled(devIsCheck);
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                devIsCheck=b;
                editor.putBoolean(name,devIsCheck);
                editor.commit();
                if(!devIsCheck)
                {
                    sIniHelper.deleteItem(name);
                    sIniHelper.save(cfgPath);

                }else{

                    if(name.equals("XHawk-0"))
                    {
                        saveUsbInFile();
                        streamState = DeviceInfo.BINDED;
                    }
                }
                mButtonCnn.setEnabled(devIsCheck);
                sIniHelper.load(cfgPath);
                updateView();

            }
        });
        if(name.equals("XHawk-0"))
        {
            mButtonCnn.setVisibility(View.GONE);
        }
     updateView();

    }


    protected View.OnClickListener onButtonCnnClicked=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            stream.close();
                BleAutoConnectManager2.addRequest((BleStream) stream, BleAutoConnectManager2.parseConnectOrder(name));

        }
    };

    public  int getStreamState(){
        return streamState;
    }
    @Override
    public void onStreamRead(IStreamable iStreamable) {

    }

    @Override
    public void onStreamStateChanged(IStreamable ss,int state) {
        switch(state){
            case StreamState.OPEN_SUCCESS:
                  BleAutoConnectManager2.removeRequest((BleStream)this.stream, BleAutoConnectManager2.parseConnectOrder(name));
                  deviceName = stream.getDeviveName();
                    address=stream.getAddress();
                  streamState = DeviceInfo.BINDED;

                saveIniFile();
                  break;
            case StreamState.SCANNING:
                  streamState = DeviceInfo.SCANNING;
                  break;
            case StreamState.OPENING:
                  break;
            case StreamState.CLOSE:
                streamState = DeviceInfo.NONE_BIND;
                break;
            case StreamState.OPEN_FAILURE:
                streamState = DeviceInfo.NONE_BIND;
                Toast.makeText(context,"扫描超时！",Toast.LENGTH_SHORT).show();
                break;
        }
        updateView();
        Log.d(TAG,"onStreamStateChanged : "+state);
    }
    public void saveIniFile()
    {
        String devices=sIniHelper.getItem("Ximmerse Service","Devices","");
        if(devices==null||devices.equals(""))
        {
            devices=name;
        }
        else devices=devices+","+name;
        sIniHelper.setItem("Ximmerse Service","Devices",devices);
        sIniHelper.setItem(name,"DisplayName",name);
        sIniHelper.setItem(name,"Enabled","1");
        sIniHelper.setItem(name,"StreamType","Ble");
        sIniHelper.setItem(name,"AutoConnect","1");
        sIniHelper.setItem(name,"DeviceName",deviceName);

        sIniHelper.setItem(name,"Address",address);
        sIniHelper.save(cfgPath);

    }

public static void saveUsbInFile()
{
    String devices=sIniHelper.getItem("Ximmerse Service","Devices","");
    String name="XHawk-0";
    if(devices==null||devices.equals(""))
        devices=name;
    else devices=devices+","+name;
    sIniHelper.setItem("Ximmerse Service","Devices",devices);

    sIniHelper.setItem(name,"StreamType","Usb");
    sIniHelper.setItem(name,"AutoConnect","1");
    sIniHelper.setItem(name,"Address","VID=0x1F3B,PID=0x0,DID=0");
    sIniHelper.setItem(name,"ReadBufferSize","512");
    sIniHelper.save(cfgPath);
}

}