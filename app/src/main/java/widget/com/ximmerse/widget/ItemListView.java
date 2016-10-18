//=============================================================================
//
// Copyright 2016 Ximmerse, LTD. All rights reserved.
//
//=============================================================================

package widget.com.ximmerse.widget;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemListView<T> {

    public interface OnItemClickListener<T>{
        void onItemClick(ItemListView<T> parent, View view, T item, HashMap<String, Object> uiRes, boolean longClick);
    }

    protected Context mContext;

    protected List<T> mItemList;
    protected ListView mItemView;
    protected SimpleAdapter mItemAdapter;

    protected List<HashMap<String,Object>> mItemUiRes;
    protected String[] mItemFields;
    protected int[] mItemFieldResIds;
    protected OnItemClickListener mOnItemClickListener;

    protected Object[] mItemIcons;

    public ItemListView(Context context){
        mContext=context;
    }

    public ItemListView(Context context, List<T> itemList, ListView listView, int itemResId, String[] fields, int[] fieldResIds){
        mContext=context;
        initItemListView(itemList,listView,itemResId,fields,fieldResIds);
    }

    public void initItemListView(List<T> itemList, ListView listView, int itemResId, String[] fields, int[] fieldResIds){

        mItemView=listView;
        mItemFields=fields;
        mItemFieldResIds=fieldResIds;

        mItemList=itemList!=null?itemList:new ArrayList<T>();
        mItemUiRes=new ArrayList<HashMap<String, Object>>();

        mItemAdapter=new SimpleAdapter(mContext,mItemUiRes,itemResId,mItemFields,mItemFieldResIds);
        mItemView.setAdapter(mItemAdapter);

        //??
        mItemView.setOnItemClickListener(onItemClickListener);
        mItemView.setOnItemLongClickListener(onItemLongClickListener);
    }

    public void updateView(){

    }

    // Properties.

    public List<T> getItemList(){
        return mItemList;
    }

    public String[] getItemFields(){
        return mItemFields;
    }

    public View getItemView(){
        return mItemView;
    }

    public void setItemView(View value){
        mItemView=(ListView)value;
    }

    public Object[] getItemIcons(){
        return mItemIcons;
    }

    public void setItemIcons(Object[] value){
        mItemIcons=value;
    }

    public void setDirty(){
        if(mItemAdapter!=null) {
            mItemAdapter.notifyDataSetChanged();
        }
    }

    public List<HashMap<String,Object>> getItemUiRes(){
        return mItemUiRes;
    }

    public void clearItemUiRes(){

    }

    // Event handler.

    public void setOnViewCreatedListener(SimpleAdapter.OnViewCreatedListener l){
        mItemAdapter.setOnViewCreatedListener(l);
    }

    public void setOnItemClickListener(OnItemClickListener<T> l){
        mOnItemClickListener=l;
    }

    protected AdapterView.OnItemClickListener onItemClickListener=new AdapterView.OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(mOnItemClickListener!=null) {
                mOnItemClickListener.onItemClick(ItemListView.this,view,mItemList.get(position),mItemUiRes.get(position),false);
            }
        }
    };

    protected AdapterView.OnItemLongClickListener onItemLongClickListener=new AdapterView.OnItemLongClickListener(){

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if(mOnItemClickListener!=null){
                mOnItemClickListener.onItemClick(ItemListView.this,view,mItemList.get(position),mItemUiRes.get(position),true);
            }
            return true;
        }
    };

}
