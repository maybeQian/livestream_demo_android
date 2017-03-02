/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.live.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cn.ucai.live.LiveHelper;


public class DbOpenHelper extends SQLiteOpenHelper{

	private static final int DATABASE_VERSION = 1;
	private static DbOpenHelper instance;

	private static final String USERNAME_TABLE_CREATE = "CREATE TABLE "
			+ UserDao.TABLE_NAME + " ("
			+ UserDao.COLUMN_NAME_NICK + " TEXT, "
			+ UserDao.COLUMN_NAME_AVATAR + " TEXT, "
			+ UserDao.COLUMN_NAME_ID + " TEXT PRIMARY KEY);";
	

	private static final String CREATE_PREF_TABLE = "CREATE TABLE "
            + UserDao.PREF_TABLE_NAME + " ("
            + UserDao.COLUMN_NAME_DISABLED_GROUPS + " TEXT, "
            + UserDao.COLUMN_NAME_DISABLED_IDS + " TEXT);";

	private static final String USER_TABLE_CREATE = "CREATE TABLE "
			+ UserDao.User_TABLE_NAME + "("
			+ UserDao.User_COLUMN_NAME + " TEXT PRIMARY KEY,"
			+ UserDao.User_COLUMN_NAME_NICK + " TEXT,"
			+ UserDao.User_COLUMN_NAME_AVATAR_ID + " INTEGER,"
			+ UserDao.User_COLUMN_NAME_AVATAR_NAME + " TEXT,"
			+ UserDao.User_COLUMN_NAME_AVATAR_SUFFIX + " TEXT,"
			+ UserDao.User_COLUMN_NAME_AVATAR_PATH + " TEXT,"
			+ UserDao.User_COLUMN_NAME_AVATAR_TYPE + " INTEGER,"
			+ UserDao.User_COLUMN_NAME_AVATAR_UPDATE_TIME + " TEXT);";

	private static final String GIFT_TABLE_CREATE = "CREATE TABLE "
			+ UserDao.GIFT_TABLE_NAME + "("
			+ UserDao.GIFT_COLUMN_ID + " INTEGER PRIMARY KEY,"
			+ UserDao.GIFT_COLUMN_NAME + " TEXT,"
			+ UserDao.GIFT_COLUMN_URL + " TEXT,"
			+ UserDao.GIFT_COLUMN_PRICE + " INTEGER);";
	private DbOpenHelper(Context context) {
		super(context, getUserDatabaseName(), null, DATABASE_VERSION);
	}
	
	public static DbOpenHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DbOpenHelper(context.getApplicationContext());
		}
		return instance;
	}
	
	private static String getUserDatabaseName() {
        return  LiveHelper.getInstance().getCurrentUsernName() + "_demo.db";
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(USERNAME_TABLE_CREATE);
		db.execSQL(CREATE_PREF_TABLE);
		db.execSQL(USER_TABLE_CREATE);
		db.execSQL(GIFT_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}


	public void closeDB() {
	    if (instance != null) {
	        try {
	            SQLiteDatabase db = instance.getWritableDatabase();
	            db.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        instance = null;
	    }
	}
	
}
