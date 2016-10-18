//=============================================================================
//
// Copyright 2016 Ximmerse, LTD. All rights reserved.
//
//=============================================================================

package com.ximmerse.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.List;

/**Reference : http://blog.csdn.net/shenyuanqing/article/details/46670761*/
public class FragmentPagerActivity extends AppCompatActivity{

    protected ViewPager mViewPager;
    protected RadioGroup mRadioGroup;
    protected int[] mRadioButtonResIds;
    protected RadioButton[] mRadioButtons;
    protected List<Fragment> mFragments;

    protected void initFragmentPager() {
        //
        int i=0,imax=mRadioButtonResIds.length;
        mRadioButtons=new RadioButton[imax];
        for(;i<imax;++i){
            mRadioButtons[i]=(RadioButton)this.findViewById(mRadioButtonResIds[i]);
        }
        //RadioGroup选中状态改变监听
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                for(int i=0,imax=mRadioButtonResIds.length;i<imax;++i){
                    if(mRadioButtonResIds[i]==checkedId) {
                        mViewPager.setCurrentItem(i, true);
                    }
                }
            }
        });

        /**
         * ViewPager部分
         */
        //ViewPager设置适配器
        mViewPager.setAdapter(new MyFragmentPagerAdapter(getSupportFragmentManager(), mFragments));
        //ViewPager显示第一个Fragment
        mViewPager.setCurrentItem(0);
        //ViewPager页面切换监听
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(mRadioButtons[position]!=null) {
                    mRadioGroup.check(mRadioButtonResIds[position]);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public void onFragmentCreateView(Fragment fragment,View view){

    }

    public static class MyFragment extends Fragment {

        public static MyFragment newInstance(int layoutResId) {
            MyFragment f = new MyFragment();
            Bundle args = new Bundle();
            args.putInt("layoutResId", layoutResId);
            f.setArguments(args);

            return f;
        }

        public int layoutResId;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.layoutResId= getArguments() != null ? getArguments().getInt("layoutResId") : 0;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(layoutResId,container, false);
            ((FragmentPagerActivity)getActivity()).onFragmentCreateView(this,v);
            return v;
        }

    }

    public class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        public List<Fragment> list;

        public MyFragmentPagerAdapter(FragmentManager fm, List<Fragment> list) {
            super(fm);
            this.list = list;
        }

        @Override
        public Fragment getItem(int position) {
            return list.get(position);
        }

        @Override
        public int getCount() {
            return list.size();
        }
    }
}
