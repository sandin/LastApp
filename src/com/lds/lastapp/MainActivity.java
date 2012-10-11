package com.lds.lastapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

public class MainActivity extends ListActivity {
    private static final String TAG = "MainActivity";

    private List<PackageInfo> packageInfoList;
    private PackageManager pm;

    private ListView listView;
    private ListAdapter listAdapter;

    private SearchView mSearchView;
    private String searchIndex = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pm = getPackageManager();
        Log.i(TAG, "t");
        packageInfoList = getOrderedPackageInfoList(pm);
        Log.i(TAG, "t");

        initView();
        onRefresh();
        new SearchIndexTask().execute();
        
        /*
        ActivityManager m = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
        List<RecentTaskInfo> recentTaskInfoList = m.getRecentTasks(50,0);
        for (RecentTaskInfo info : recentTaskInfoList) {
            if (info.origActivity != null ) {
                Log.i(TAG, "info : " + info.origActivity.getPackageName());
            }
            Log.i(TAG, "intent: " + info.baseIntent.toString());
        }
        */
    }

    private void initView() {
        listView = getListView();
        listAdapter = new ListAdapter(this);
        listView.setAdapter(listAdapter);
        
        getActionBar();
    }

    public void onRefresh() {
        listAdapter.refresh(packageInfoList.subList(0, 20));
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long id) {
                PackageInfo item = (PackageInfo) listAdapter.getItem(position);

                Intent intent = pm.getLaunchIntentForPackage(item.packageName);
                startActivity(intent);
                finish();
            }
        });
    }
    
    private class SearchIndexTask extends AsyncTask<Void, Void, String> {
        
        @Override
        protected String doInBackground(Void... params) {
            return createSearchIndex(packageInfoList, pm);
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            searchIndex = result;
            Toast.makeText(getApplicationContext(), "创建索引成功!", Toast.LENGTH_SHORT).show();
        }
    }

    // 创建搜索索引
    private String createSearchIndex(List<PackageInfo> packageInfoList, 
            PackageManager pm) {
        Log.i(TAG, "i");
        StringBuilder sb = new StringBuilder();
        for (int i = 0, l = packageInfoList.size(); i < l; i++) {
            PackageInfo info = packageInfoList.get(i);
            //Log.i(TAG, info.packageName); 
            //Log.i(TAG, pm.getApplicationLabel(info.applicationInfo) + "");
            
            // #package@i#
            sb.append("#");
            sb.append(info.packageName);
            sb.append("@");
            sb.append(i);
            sb.append("#");
            
            String appLabel = pm.getApplicationLabel(info.applicationInfo).toString();
            
            // #name@i#
            sb.append("#");
            sb.append(appLabel);
            sb.append("@");
            sb.append(i);
            sb.append("#");
            
            // #pingyin@i#
            sb.append("#");
            sb.append(Utils.toPinyin(appLabel));
            sb.append("@");
            sb.append(i);
            sb.append("#");
        }
        return sb.toString();
    }
    
    // 搜索，并将结果显示到UI
    private boolean search(String input) {
        Log.i(TAG, "search for " + input);
        if (searchIndex == null) {
            Toast.makeText(getApplicationContext(), "正在创建索引，请稍候", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        List<PackageInfo> resultList = new ArrayList<PackageInfo>();
        
        Pattern pattern=Pattern.compile("#([^#]*" + input + "[^#]*)#");  
        Matcher matcher = pattern.matcher(searchIndex);  
        while (matcher.find()) {  
            String m = matcher.group(1); // TODO
            String[] tmp = m.split("@");
            if (tmp.length == 2) {
                int index = Integer.parseInt(tmp[1]);
                if (index >= 0 && index < packageInfoList.size()) {
                    PackageInfo item = packageInfoList.get(index);
                    Log.i(TAG, "found: " + pm.getApplicationLabel(item.applicationInfo));
                    if (! resultList.contains(item)) { // 去除重复项
                        resultList.add(item);
                    }
                }
            }
        }
        listAdapter.refresh(resultList);
        return (resultList.size() > 0);
    }

    // 排序
    private List<PackageInfo> getOrderedPackageInfoList(PackageManager pm) {
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
        Collections.sort(packageInfoList, new ComparatorPackage()); // sort by
                                                                    // time
        return packageInfoList;
    }

    public class ComparatorPackage implements Comparator<PackageInfo> {

        public int compare(PackageInfo arg0, PackageInfo arg1) {
                if (arg1.firstInstallTime > arg0.firstInstallTime)
                    return 1;
                else if (arg1.firstInstallTime < arg0.firstInstallTime)
                    return -1;
                else
                    return 0;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        mSearchView = (SearchView) menu.findItem(R.id.search).getActionView();
        mSearchView.setIconifiedByDefault(false);
        mSearchView.requestFocus();
        setupSearchView();
        return true;
    }

    private void setupSearchView() {
        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            
            @Override
            public boolean onQueryTextSubmit(String query) {
                return search(query);
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                return search(newText);
            }
        });
    }

    /**
     * Adapter for The list
     */
    class ListAdapter extends BaseAdapter {
        private List<PackageInfo> list;

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
