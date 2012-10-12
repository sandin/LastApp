package com.lds.lastapp;

import java.util.ArrayList;
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

import com.lds.lastapp.db.LastAppDatabase;
import com.lds.lastapp.model.AppInfo;

public class MainActivity extends ListActivity implements OnItemClickListener {
    private static final String TAG = "MainActivity";
    
    private static final int MAX_DISPLAY_ITEM = 20; // 最大结果集，显示过多结果没有意义
    
    private static final String SP_KEY_SEARCH_INDEX = "search_index";
    private SharedPreferences sp;

    private List<AppInfo> appInfoList; // 所有包信息
    private PackageManager pm;

    private ListView listView;
    private ListAdapter listAdapter;

    private SearchView mSearchView;
    private String searchIndex = null;

    private SearchIndexTask searchIndexTask;
    private GetAppInfoTask getAppInfoTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        pm = getPackageManager();
        sp = getPreferences(Context.MODE_PRIVATE);
        
        getAppInfoTask = new GetAppInfoTask();
        getAppInfoTask.execute();

        searchIndexTask = new SearchIndexTask();
        searchIndexTask.execute();
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

    public void onRefresh(List<AppInfo> packageInfoList) {
        listAdapter.refresh(
                (packageInfoList.size() > MAX_DISPLAY_ITEM)
                ? packageInfoList.subList(0, MAX_DISPLAY_ITEM)
                : packageInfoList
        );
    }
    
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1,
            int position, long id) {
        final AppInfo item = (AppInfo) listAdapter.getItem(position);
        if (item == null) {
            return;
        }
        
        // 累计运行次数
        new Thread() {
            public void run() {
                LastAppDatabase database = LastAppDatabase.getInstance(getApplicationContext());
                database.updateRunCount(item);
            };
        }.start();

        Intent intent = pm.getLaunchIntentForPackage(item.packageName);
        startActivity(intent);
        finish();
    }
    
    // TODO: 是否每次都需要刷新索引?
    private class SearchIndexTask extends AsyncTask<Void, Void, String> {
        
        @Override
        protected void onPreExecute() {
            searchIndex = restoreCacheSearchIndex(); // 先使用缓存
        }
        
        @Override
        protected String doInBackground(Void... params) {
            if (appInfoList != null) {
                String searchIndex = createSearchIndex(appInfoList, pm);
                storeSearchIndex(searchIndex); // cache it
                return searchIndex;
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                searchIndex = result;
            }
            //Toast.makeText(getApplicationContext(), getString(R.string.create_search_index_successed), Toast.LENGTH_SHORT).show();
        }
    }

    // 创建搜索索引
    private String createSearchIndex(List<AppInfo> packageInfoList, 
            PackageManager pm) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        StringBuilder sb3 = new StringBuilder();
        for (int i = 0, l = packageInfoList.size(); i < l; i++) {
            AppInfo info = packageInfoList.get(i);
            //Log.i(TAG, info.packageName); 
            //Log.i(TAG, pm.getApplicationLabel(info.applicationInfo) + "");
            
            // #name@i#
            sb.append("#");
            sb.append(info.applicationLabel);
            sb.append("@");
            sb.append(i);
            sb.append("#");
            
            // #pinyin@i#
            sb.append("#");
            sb.append(info.getApplicationLabelPinYin());
            sb.append("@");
            sb.append(i);
            sb.append("#");
            
            // #package@i#
            sb.append("#");
            sb.append(info.packageName);
            sb.append("@");
            sb.append(i);
            sb.append("#");
        }
        //sb.append(sb2).append(sb3); // 确保拼音的排在英文和中文之后
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
        if (appInfoList == null) {
            return false;
        }
        
        List<AppInfo> resultList = new ArrayList<AppInfo>();
        Pattern pattern=Pattern.compile("#([^#]*" + input.toLowerCase() + "[^#]*)#");  
        Matcher matcher = pattern.matcher(searchIndex);  
        while (matcher.find()) {  
            String m = matcher.group(1); // TODO
            String[] tmp = m.split("@");
            if (tmp.length == 2) {
                int index = Integer.parseInt(tmp[1]);
                if (index >= 0 && index < appInfoList.size()) {
                    AppInfo item = appInfoList.get(index);
                    Log.i(TAG, "found: " + item.applicationLabel);
                    if (! resultList.contains(item)) { // 去除重复项
                        resultList.add(item);
                    }
                }
            }
        }
        
        onRefresh(resultList);
        return (resultList.size() > 0);
    }
    
    private class GetAppInfoTask extends AsyncTask<Void, Integer, List<AppInfo>> {
        private static final int GET_CACHE = 1;
        private static final int GET_REALTIME = 2;
        
        private List<AppInfo> list;

        @Override
        protected List<AppInfo> doInBackground(Void... params) {
            LastAppDatabase database = LastAppDatabase.getInstance(getApplicationContext());
            
            list = database.getAllAppInfo(getApplicationContext());
            this.publishProgress(GET_CACHE);
            
            List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
            database.saveAppInfoList(getApplicationContext(), packageInfoList, pm);
            
            list = database.getAllAppInfo(getApplicationContext());
            this.publishProgress(GET_REALTIME);
                
            return list;
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            
            switch (values[0]) {
            case GET_CACHE:
                appInfoList = list;
                onRefresh(appInfoList);
            case GET_REALTIME:
                if (! appInfoList.equals(list)) {
                    appInfoList = list;
                    onRefresh(appInfoList);
                }
            }
        }
        
        @Override
        protected void onPostExecute(List<AppInfo> result) {
            super.onPostExecute(result);
            
            //appInfoList = result;
            //onRefresh(appInfoList);
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
        private List<AppInfo> list;

        public ListAdapter(Context context) {
            list = new ArrayList<AppInfo>();
        }

        public void refresh(List<AppInfo> list) {
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

            AppInfo item = list.get(position);
            Holder holder = (Holder) view.getTag();
            holder.text.setText(item.getApplicationLabel());
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
