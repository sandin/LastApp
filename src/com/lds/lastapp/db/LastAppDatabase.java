package com.lds.lastapp.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.lds.lastapp.Utils;
import com.lds.lastapp.model.AppInfo;

public class LastAppDatabase {
    private static final String TAG = "LastAppDatabase";

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "app.db";
    
    public static final String APP_TABLE_NAME = "apps";
    public static final String HISTORY_TABLE_NAME = "historys";
    
    private MyOpenHelper mOpenHelper;
    
    private static LastAppDatabase mInstance;
    public static LastAppDatabase getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LastAppDatabase(context);
        }
        return mInstance;
    }
    
    private LastAppDatabase(Context context) {
        mOpenHelper = new MyOpenHelper(context);
    }
    
    public SQLiteDatabase getDb(boolean writeable) {
        if (writeable) {
            return mOpenHelper.getWritableDatabase();
        } else {
            return mOpenHelper.getReadableDatabase();
        }
    }

    public void close() {
        mOpenHelper.close();
    }
    
    public interface AppColumns extends BaseColumns {
        public static final String KEY_APPLICATION_LABEL = "applicationLabel";
        public static final String KEY_PACKAGE_NAME = "packageName";
        public static final String KEY_FIRST_INSTALL_TIME = "firstInstallTime";
        public static final String KEY_LAST_UPDATE_TIME = "lastUpdateTime";
        public static final String KEY_APPLICATION_LABEL_PINYIN = "applicationLabelPinYin";
    }
    
    public interface HistoryColumns extends BaseColumns {
        public static final String KEY_PACKAGE_NAME = "packageName";
        public static final String KEY_WEIGHT = "weight";
        public static final String KEY_RUN_COUNT = "runCount";
        public static final String KEY_FIXED = "fixed";
        public static final String KEY_LAST_RUN_TIME = "lastRunTime";
    }
   
    private class MyOpenHelper extends SQLiteOpenHelper {

        private static final String APP_TABLE_CREATE =
                    "CREATE TABLE " + APP_TABLE_NAME + " (" +
                    AppColumns._ID + " INTEGER PRIMARY KEY, " +
                    AppColumns.KEY_APPLICATION_LABEL + " TEXT, " +
                    AppColumns.KEY_APPLICATION_LABEL_PINYIN + " TEXT, " +
                    AppColumns.KEY_PACKAGE_NAME + " TEXT, " +
                    AppColumns.KEY_FIRST_INSTALL_TIME + " DATETIME, " +
                    AppColumns.KEY_LAST_UPDATE_TIME + " DATETIME " +
                    ");";
        
        private static final String HISTORY_TABLE_CREATE = 
                "CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
                    HistoryColumns._ID + " INTEGER PRIMARY KEY, " +
                    HistoryColumns.KEY_PACKAGE_NAME + " TEXT, " +
                    HistoryColumns.KEY_WEIGHT + " INT, " +
                    HistoryColumns.KEY_RUN_COUNT + " INT, " +
                    HistoryColumns.KEY_FIXED + " INT, " +
                    HistoryColumns.KEY_LAST_RUN_TIME + " DATETIME " +
                    ");";
        

        public MyOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "Create Tables");
            db.execSQL(APP_TABLE_CREATE);
            db.execSQL(HISTORY_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
        }
    }
    
    public int saveAppInfoList(Context context, List<PackageInfo> list, final PackageManager pm) {
        Log.i(TAG, "Save App Info List. " + list.size());
        SQLiteDatabase db = getDb(true);
        db.delete(APP_TABLE_NAME, null, null); // 清空表
        int count = 0;
        try {
            //db.beginTransaction();
            
            Log.i(TAG, "list size: " + list.size());
            for (int i = 0, l = list.size(); i < l; i++) {
                PackageInfo item = list.get(i);
                ContentValues v = new ContentValues();
                String appLable = pm.getApplicationLabel(item.applicationInfo).toString();
                v.put(AppColumns.KEY_APPLICATION_LABEL, appLable);
                try {
                    v.put(AppColumns.KEY_APPLICATION_LABEL_PINYIN, Utils.toPinyin(appLable));
                } catch (Exception e) {
                    e.printStackTrace();
                    v.put(AppColumns.KEY_APPLICATION_LABEL_PINYIN, "");
                }
                v.put(AppColumns.KEY_FIRST_INSTALL_TIME, item.firstInstallTime);
                v.put(AppColumns.KEY_LAST_UPDATE_TIME, item.lastUpdateTime);
                v.put(AppColumns.KEY_PACKAGE_NAME, item.packageName);
                //v.put(AppColumns.KEY_LAST_RUN_TIME, 0);
                //v.put(AppColumns.KEY_WEIGHT, false);
                //v.put(AppColumns.KEY_RUN_COUNT, 0);
                //v.put(AppColumns.KEY_WEIGHT, 0);
                long rowId = db.insert(APP_TABLE_NAME, null, v);
                if (rowId > 0) {
                    //Log.d(TAG, "insert the appInfo: " + item.toString());
                    count++;
                } else {
                    Log.e(TAG, "insert fail. " + item.toString());
                }
            }
            //db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            //db.endTransaction();
        }
        return count;
    }
    
    public boolean saveOrUpdateHistory(String packageName) {
        SQLiteDatabase db = getDb(true);
        
        Cursor c = db.query(HISTORY_TABLE_NAME,
                new String[] {HistoryColumns.KEY_RUN_COUNT, HistoryColumns.KEY_WEIGHT},
                HistoryColumns.KEY_PACKAGE_NAME + " = ?",
                new String[] { packageName }, null, null, null);
        
        ContentValues values = new ContentValues();
        long result = 0;
        if (c.moveToNext()) { // update
            Log.v(TAG, "Update history for package: " + packageName);
            int runCount = c.getInt(c.getColumnIndex(HistoryColumns.KEY_RUN_COUNT));
            int weight = c.getInt(c.getColumnIndex(HistoryColumns.KEY_WEIGHT));
            values.put(HistoryColumns.KEY_RUN_COUNT, runCount+1);
            values.put(HistoryColumns.KEY_WEIGHT, weight+1);
            result = db.update(HISTORY_TABLE_NAME, values,
                    HistoryColumns.KEY_PACKAGE_NAME + "=?",
                    new String[] {packageName} );
        } else { // save
            Log.v(TAG, "Save history for package: " + packageName);
            values.put(HistoryColumns.KEY_FIXED, false);
            values.put(HistoryColumns.KEY_LAST_RUN_TIME, System.currentTimeMillis());
            values.put(HistoryColumns.KEY_PACKAGE_NAME, packageName);
            values.put(HistoryColumns.KEY_RUN_COUNT, 1);
            values.put(HistoryColumns.KEY_WEIGHT, 1);
            result = db.insert(HISTORY_TABLE_NAME, null, values);
        }
        c.close();
        db.close();
        return result > 0;
    }
    
    private static final String SQL_GET_ALL_APP_INFO = "SELECT "
                    + "  a." + AppColumns._ID
                    + ", a." + AppColumns.KEY_APPLICATION_LABEL
                    + ", a." + AppColumns.KEY_APPLICATION_LABEL_PINYIN
                    + ", a." + AppColumns.KEY_FIRST_INSTALL_TIME
                    + ", a." + AppColumns.KEY_LAST_UPDATE_TIME
                    + ", a." + AppColumns.KEY_PACKAGE_NAME
                    + ", h." + HistoryColumns.KEY_FIXED
                    + ", h." + HistoryColumns.KEY_LAST_RUN_TIME
                    + ", h." + HistoryColumns.KEY_RUN_COUNT
                    + ", h." + HistoryColumns.KEY_WEIGHT
                    + " FROM " + APP_TABLE_NAME + " AS a"
                    + " LEFT JOIN " + HISTORY_TABLE_NAME + " AS h "
                    + " ON a."+ AppColumns.KEY_PACKAGE_NAME + "="
                    + "h."+HistoryColumns.KEY_PACKAGE_NAME
                    + " ORDER BY " 
                    + "h."+HistoryColumns.KEY_WEIGHT + " DESC, "
                    + "a."+AppColumns.KEY_FIRST_INSTALL_TIME + " DESC ";
    
    public List<AppInfo> getAllAppInfo(Context context) {
        List<AppInfo> list = new ArrayList<AppInfo>();
        SQLiteDatabase db = getDb(false);
        Log.i(TAG, SQL_GET_ALL_APP_INFO);
        Cursor c = db.rawQuery(SQL_GET_ALL_APP_INFO, null);
        
        while (c.moveToNext()) {
            AppInfo bean = new AppInfo();
            bean.setApplicationLabel(c.getString(c.getColumnIndex(AppColumns.KEY_APPLICATION_LABEL)));
            bean.setApplicationLabelPinYin(c.getString(c.getColumnIndex(AppColumns.KEY_APPLICATION_LABEL_PINYIN)));
            bean.setFirstInstallTime(c.getLong(c.getColumnIndex(AppColumns.KEY_FIRST_INSTALL_TIME)));
            bean.setId(c.getLong(c.getColumnIndex(AppColumns._ID)));
            bean.setLastRunTime(c.getLong(c.getColumnIndex(HistoryColumns.KEY_LAST_RUN_TIME)));
            bean.setLastUpdateTime(c.getLong(c.getColumnIndex(AppColumns.KEY_LAST_UPDATE_TIME)));
            bean.setPackageName(c.getString(c.getColumnIndex(AppColumns.KEY_PACKAGE_NAME)));
            bean.setFixed(c.getInt(c.getColumnIndex(HistoryColumns.KEY_FIXED)) == 1);
            bean.setRunCount(c.getInt(c.getColumnIndex(HistoryColumns.KEY_RUN_COUNT)));
            bean.setWeight(c.getInt(c.getColumnIndex(HistoryColumns.KEY_WEIGHT)));
            list.add(bean);
        }
        c.close();
        db.close();
        Log.v(TAG, "Get All AppInfo List: " + list.size());
        return list;
    }
}
