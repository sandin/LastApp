package com.lds.lastapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends ListActivity {
	private static final String TAG = "MainActivity";
	
	private List<PackageInfo> packageInfoList;
    private PackageManager pm;
    
    private ListView listView;
    private ListAdapter listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
    	pm = getPackageManager();
    	packageInfoList = getOrderedPackageInfoList(pm);
    	
    	initView();
    	
    	onRefresh();
    }
    
    private void initView() {
    	listView = getListView();
    	listAdapter = new ListAdapter(this);
    	listView.setAdapter(listAdapter);
    }

	public void onRefresh() {
		listAdapter.refresh(packageInfoList.subList(0, 50));
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				PackageInfo item = (PackageInfo) listAdapter.getItem(position);
				
				Intent intent = pm.getLaunchIntentForPackage(item.packageName);
				startActivity(intent);
				finish();
			}
		});
		
		/*
		for (PackageInfo info : packageInfoList) {
			// Log.i(TAG, info.toString());
			// Log.i(TAG, info.packageName);
			Log.i(TAG, info.firstInstallTime + "");
			// Log.i(TAG, info.lastUpdateTime +"");
			// Log.i(TAG, pm.getInstallerPackageName(info.packageName) + "");
			try {
				Drawable logo = pm.getApplicationLogo(info.packageName);
				Drawable icon = pm.getApplicationIcon(info.packageName);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i(TAG, pm.getApplicationLabel(info.applicationInfo).toString()
					+ "");
			Intent i = pm.getLaunchIntentForPackage(info.packageName);
		}
		*/
	}
    
    private List<PackageInfo> getOrderedPackageInfoList(PackageManager pm) {
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
        Collections.sort(packageInfoList, new ComparatorPackage()); // sort by time
        return packageInfoList;
    }
    
    public class ComparatorPackage implements Comparator<PackageInfo> {
    	
    	public int compare(PackageInfo arg0, PackageInfo arg1) {
    		 if (arg1.firstInstallTime > arg0.firstInstallTime)
    	         return 1;
    	     else if(arg1.firstInstallTime < arg0.firstInstallTime)
    	         return -1;
    	     else
    	    	 return 0;
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    /**
     * 
     */
    class ListAdapter extends BaseAdapter {
    	private List<PackageInfo> list;
    	private Context context;
    	
    	public ListAdapter(Context context) {
    		list = new ArrayList<PackageInfo>();
    	}
    	
    	public void refresh(List<PackageInfo> list) {
    		this.list.clear();
    		this.list.addAll(list);
    		notifyDataSetChanged();
    	}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = getLayoutInflater().inflate(R.layout.list_item, null);
				Holder holder = new Holder();
				holder.icon = (ImageView) view.findViewById(R.id.icon);
				holder.text = (TextView) view.findViewById(R.id.text);
				view.setTag(holder);
			} else {
				view = convertView;
			}
			
			PackageInfo item = list.get(position);
			Holder holder = (Holder) view.getTag();
			holder.text.setText(pm.getApplicationLabel(item.applicationInfo));
			try {
				Drawable drawable = pm.getApplicationIcon(item.packageName);
				holder.icon.setImageDrawable(drawable);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			
			return view;
		}
		
		class Holder {
			ImageView icon;
			TextView text;
		}
    	
    }

    
}
