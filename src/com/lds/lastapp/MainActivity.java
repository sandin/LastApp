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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity implements OnItemClickListener {
    private static final String TAG = "MainActivity";
    
    private static final int MAX_DISPLAY_ITEM = 20; // 最大结果集，显示过多结果没有意义
    
    private static final String SP_KEY_SEARCH_INDEX = "search_index";
    private SharedPreferences sp;

    private List<PackageInfo> packageInfoList; // 所有包信息
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
        packageInfoList = getOrderedPackageInfoList(pm);
        sp = getPreferences(Context.MODE_PRIVATE);

        initView();
        onRefresh(packageInfoList);
        
        searchIndex = restoreCacheSearchIndex(); // 先从缓存中读取索引, 然后刷新索引
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
    
    private String restoreCacheSearchIndex() {
        return sp.getString(SP_KEY_SEARCH_INDEX, null);
    }
    
    private void storeSearchIndex(String searchIndex) {
        Editor editor = sp.edit();
        editor.putString(SP_KEY_SEARCH_INDEX, searchIndex);
        editor.commit();
    }

    private void initView() {
        listView = getListView();
        listAdapter = new ListAdapter(this);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this);
        
        getActionBar();
    }

    public void onRefresh(List<PackageInfo> packageInfoList) {
        listAdapter.refresh(
                (packageInfoList.size() > MAX_DISPLAY_ITEM)
                ? packageInfoList.subList(0, MAX_DISPLAY_ITEM)
                : packageInfoList
        );
    }
    
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1,
            int position, long id) {
        PackageInfo item = (PackageInfo) listAdapter.getItem(position);

        Intent intent = pm.getLaunchIntentForPackage(item.packageName);
        startActivity(intent);
        finish();
    }
    
    private class SearchIndexTask extends AsyncTask<Void, Void, String> {
        
        @Override
        protected String doInBackground(Void... params) {
            String searchIndex = createSearchIndex(packageInfoList, pm);
            storeSearchIndex(searchIndex); // cache it
            return searchIndex;
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            searchIndex = result;
            //Toast.makeText(getApplicationContext(), getString(R.string.create_search_index_successed), Toast.LENGTH_SHORT).show();
        }
    }

    // 创建搜索索引
    private String createSearchIndex(List<PackageInfo> packageInfoList, 
            PackageManager pm) {
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
            
            // #pinyin@i#
            sb.append("#");
            sb.append(Utils.toPinyin(appLabel));
            sb.append("@");
            sb.append(i);
            sb.append("#");
        }
        return sb.toString().toLowerCase();
    }
    
    /**
     * 搜索关键词，并将结果显示到UI
     * 
     * @param input 关键词， 大小写不敏感
     * @return 是否至少找到一个结果
     */
    private boolean search(String input) {
        Log.i(TAG, "search for " + input);
        if (searchIndex == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.create_search_index_doing), Toast.LENGTH_SHORT).show();
            return false;
        }
        
        List<PackageInfo> resultList = new ArrayList<PackageInfo>();
        Pattern pattern=Pattern.compile("#([^#]*" + input.toLowerCase() + "[^#]*)#");  
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
        
        onRefresh(resultList);
        return (resultList.size() > 0);
    }

    // 排序
    private List<PackageInfo> getOrderedPackageInfoList(PackageManager pm) {
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
        Collections.sort(packageInfoList, new ComparatorPackage()); // sort by time
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
        mSearchView.setQueryHint(getString(R.string.search_hit));
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_GO);
        setupSearchView();
        return true;
    }

    private void setupSearchView() {
        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return openTheFirstItem(query);
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                return search(newText);
            }
        });
    }
    
    private boolean openTheFirstItem(String query) {
        if (listAdapter.getCount() > 0) {
            onItemClick(null, null, 0 /* firstItem */, 0);
            return true;
        }
        return false;
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
