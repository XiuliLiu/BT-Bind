//=============================================================================
//
// Copyright 2016 Ximmerse, LTD. All rights reserved.
//
//=============================================================================

package com.ximmerse.xtools;


import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.ximmerse.app.FragmentPagerActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import widget.com.ximmerse.widget.ItemListView;
import widget.com.ximmerse.widget.SimpleAdapter;

public class MainActivity extends FragmentPagerActivity {

    protected static final String TAG = MainActivity.class.getSimpleName();


    protected List<DeviceInfo> mDeviceInfoList;
    //
    protected ItemListView<DeviceInfo> mDeviceInfoView;
    protected Object[] mDeviceInfoTypeIcons;

    // <!--

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        mFragments=new ArrayList<Fragment>();
        mFragments.add(MyFragment.newInstance(R.layout.fragment_list_view));
       // mFragments.add(MyFragment.newInstance(R.layout.fragment_list_view));
       // mFragments.add(MyFragment.newInstance(R.layout.fragment_list_view));

        mViewPager=(ViewPager)this.findViewById(R.id.viewPager0);

        mRadioGroup=(RadioGroup)this.findViewById(R.id.radioGroup_Tab0);
        mRadioButtonResIds=new int[]{};
        initFragmentPager();

        mDeviceInfoList=new ArrayList<DeviceInfo>();
        DeviceInfo.loadInI(this);
        mDeviceInfoList.add(new DeviceInfo(this, "XCobra-0"));
        mDeviceInfoList.add(new DeviceInfo(this, "XCobra-1"));
        mDeviceInfoList.add(new DeviceInfo(this, "XHawk-0"));
        mViewPager.setCurrentItem(1, true);

    }

    @Override
    protected void onDestroy() {

        if(mDeviceInfoView!=null){
            mDeviceInfoView.clearItemUiRes();
            mDeviceInfoView=null;
        }
        //
        super.onDestroy();
    }

    @Override
    public void onFragmentCreateView(Fragment fragment,View view) {
        super.onFragmentCreateView(fragment,view);
       // switch (mFragments.indexOf(fragment)) {
            // Apps
            //case 0:
            //    createPackageInfoView(fragment,view);
           // break;
            // Devices
           // case 1:
               createDeviceInfoView(fragment,view);
            //break;
            // Settings
            //case 2:
            //    ((TextView) view.findViewById(R.id.textView0)).setText("Settings and tests.");
           // break;
           // default:
           // break;
       // }
    }



    public void createDeviceInfoView(Fragment fragment,View view) {
        Log.d(TAG,"createDeviceInfoView");
        if(mDeviceInfoView==null){
            //
            if(mDeviceInfoTypeIcons==null) {
                mDeviceInfoTypeIcons = new Object[]{R.mipmap.controller_off,R.mipmap.controller_on};
            }
            //
            String[] fields=new String[]{"DevIcon","DevName0","DevState", "DevName", "DevAddr"};
            List<HashMap<String,Object>> itemUiRes;
            //
            mDeviceInfoView=new ItemListView<DeviceInfo>(
                    this,mDeviceInfoList,(ListView) view.findViewById(R.id.listView0),R.layout.item_device_info,
                    fields,
                    new int[]{R.id.imageView_DevIcon0,R.id.textView_DevName0,R.id.textView_DevState,R.id.textView_DevName, R.id.textView_DevAddr}
            );
            //
            itemUiRes=mDeviceInfoView.getItemUiRes();
            //
            itemUiRes.add(new HashMap<String, Object>());
            itemUiRes.add(new HashMap<String, Object>());
            itemUiRes.add(new HashMap<String, Object>());
            //
            mDeviceInfoView.setItemIcons(mDeviceInfoTypeIcons);
            mDeviceInfoView.setOnViewCreatedListener(mOnDeviceInfoViewCreated);
        }else {
            mDeviceInfoView.clearItemUiRes();
            mDeviceInfoView.setItemView((ListView) view.findViewById(R.id.listView0));
        }
        ((TextView) view.findViewById(R.id.textView0)).setText("Manage your devices.");

        for(DeviceInfo info:mDeviceInfoList){
            info.parentView=mDeviceInfoView;
            info.updateInfoView(this, mDeviceInfoView);

        }


    }

    protected SimpleAdapter.OnViewCreatedListener mOnDeviceInfoViewCreated=new SimpleAdapter.OnViewCreatedListener(){
        @Override
        public void onViewCreated(int position,View view){
            DeviceInfo info=mDeviceInfoList.get(position);
            if(info!=null){
                info.onViewCreated(position,view);
            }
        }
    };
}
