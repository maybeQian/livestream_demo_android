package cn.ucai.live.data.local;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseCommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import cn.ucai.live.LiveApplication;
import cn.ucai.live.LiveConstants;


public class LiveDBManager {
    static private LiveDBManager dbMgr = new LiveDBManager();
    private DbOpenHelper dbHelper;
    
    private LiveDBManager(){
        dbHelper = DbOpenHelper.getInstance(LiveApplication.getInstance().getApplicationContext());
    }
    
    public static synchronized LiveDBManager getInstance(){
        if(dbMgr == null){
            dbMgr = new LiveDBManager();
        }
        return dbMgr;
    }
    
    /**
     * save contact list
     * 
     * @param contactList
     */
    synchronized public void saveContactList(List<EaseUser> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(UserDao.TABLE_NAME, null, null);
            for (EaseUser user : contactList) {
                ContentValues values = new ContentValues();
                values.put(UserDao.COLUMN_NAME_ID, user.getUsername());
                if(user.getNick() != null)
                    values.put(UserDao.COLUMN_NAME_NICK, user.getNick());
                if(user.getAvatar() != null)
                    values.put(UserDao.COLUMN_NAME_AVATAR, user.getAvatar());
                db.replace(UserDao.TABLE_NAME, null, values);
            }
        }
    }

    synchronized public void saveAppContactList(List<User> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(UserDao.TABLE_NAME, null, null);
            for (User user : contactList) {
                ContentValues values = new ContentValues();
                values.put(UserDao.User_COLUMN_NAME, user.getMUserName());
                if(user.getMUserNick() != null)
                    values.put(UserDao.User_COLUMN_NAME_NICK, user.getMUserNick());
                if(user.getMAvatarId() != null)
                    values.put(UserDao.User_COLUMN_NAME_AVATAR_ID, user.getMAvatarId());
                if(user.getMAvatarSuffix() != null)
                    values.put(UserDao.User_COLUMN_NAME_AVATAR_SUFFIX, user.getMAvatarSuffix());
                if(user.getMAvatarPath() != null)
                    values.put(UserDao.User_COLUMN_NAME_AVATAR_PATH, user.getMAvatarPath());
                if(user.getMAvatarType() != null)
                    values.put(UserDao.User_COLUMN_NAME_AVATAR_TYPE, user.getMAvatarType());
                if(user.getMAvatarLastUpdateTime() != null)
                    values.put(UserDao.User_COLUMN_NAME_AVATAR_UPDATE_TIME, user.getMAvatarLastUpdateTime());
                db.replace(UserDao.User_TABLE_NAME, null, values);
            }
        }
    }

    /**
     * get contact list
     * 
     * @return
     */
    synchronized public Map<String, EaseUser> getContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, EaseUser> users = new Hashtable<String, EaseUser>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + UserDao.TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {
                String username = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_ID));
                String nick = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_NICK));
                String avatar = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_AVATAR));
                EaseUser user = new EaseUser(username);
                user.setNick(nick);
                user.setAvatar(avatar);
                if (username.equals(LiveConstants.NEW_FRIENDS_USERNAME) || username.equals(LiveConstants.GROUP_USERNAME)
                        || username.equals(LiveConstants.CHAT_ROOM)|| username.equals(LiveConstants.CHAT_ROBOT)) {
                        user.setInitialLetter("");
                } else {
                    EaseCommonUtils.setUserInitialLetter(user);
                }
                users.put(username, user);
            }
            cursor.close();
        }
        return users;
    }

    synchronized public Map<String, User> getAppContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, User> users = new Hashtable<String, User>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + UserDao.User_TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {
                User user=new User();
                user.setMUserName(cursor.getString(cursor.getColumnIndex(UserDao.User_COLUMN_NAME)));
                user.setMUserNick(cursor.getString(cursor.getColumnIndex(UserDao.User_COLUMN_NAME_NICK)));
                user.setMAvatarId(cursor.getInt(cursor.getColumnIndex(UserDao.User_COLUMN_NAME_AVATAR_ID)));
                user.setMAvatarSuffix(cursor.getString(cursor.getColumnIndex(UserDao.User_COLUMN_NAME_AVATAR_SUFFIX)));
                user.setMAvatarPath(cursor.getString(cursor.getColumnIndex(UserDao.User_COLUMN_NAME_AVATAR_PATH)));
                user.setMAvatarType(cursor.getInt(cursor.getColumnIndex(UserDao.User_COLUMN_NAME_AVATAR_TYPE)));
                user.setMAvatarLastUpdateTime(cursor.getString(cursor.getColumnIndex(UserDao.User_COLUMN_NAME_AVATAR_UPDATE_TIME)));
                EaseCommonUtils.setUserInitialLetter(user);
                users.put(user.getMUserName(), user);
            }
            cursor.close();
        }
        return users;
    }

    /**
     * delete a contact
     * @param username
     */
    synchronized public void deleteContact(String username){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            db.delete(UserDao.TABLE_NAME, UserDao.COLUMN_NAME_ID + " = ?", new String[]{username});
        }
    }

    synchronized public void deleteAppContact(String username){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            db.delete(UserDao.User_TABLE_NAME, UserDao.User_COLUMN_NAME + " = ?", new String[]{username});
        }
    }

    /**
     * save a contact
     * @param user
     */
    synchronized public void saveContact(EaseUser user){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.COLUMN_NAME_ID, user.getUsername());
        if(user.getNick() != null)
            values.put(UserDao.COLUMN_NAME_NICK, user.getNick());
        if(user.getAvatar() != null)
            values.put(UserDao.COLUMN_NAME_AVATAR, user.getAvatar());
        if(db.isOpen()){
            db.replace(UserDao.TABLE_NAME, null, values);
        }
    }

    synchronized public void saveAppContact(User user){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.User_COLUMN_NAME, user.getMUserName());
        if(user.getMUserNick() != null)
            values.put(UserDao.User_COLUMN_NAME_NICK, user.getMUserNick());
        if(user.getMAvatarId() != null)
            values.put(UserDao.User_COLUMN_NAME_AVATAR_ID, user.getMAvatarId());
        if(user.getMAvatarSuffix() != null)
            values.put(UserDao.User_COLUMN_NAME_AVATAR_SUFFIX, user.getMAvatarSuffix());
        if(user.getMAvatarPath() != null)
            values.put(UserDao.User_COLUMN_NAME_AVATAR_PATH, user.getMAvatarPath());
        if(user.getMAvatarType() != null)
            values.put(UserDao.User_COLUMN_NAME_AVATAR_TYPE, user.getMAvatarType());
        if(user.getMAvatarLastUpdateTime() != null)
            values.put(UserDao.User_COLUMN_NAME_AVATAR_UPDATE_TIME, user.getMAvatarLastUpdateTime());
        if(db.isOpen()){
            db.replace(UserDao.User_TABLE_NAME, null, values);
        }
    }

    public void setDisabledGroups(List<String> groups){
        setList(UserDao.COLUMN_NAME_DISABLED_GROUPS, groups);
    }
    
    public List<String>  getDisabledGroups(){       
        return getList(UserDao.COLUMN_NAME_DISABLED_GROUPS);
    }
    
    public void setDisabledIds(List<String> ids){
        setList(UserDao.COLUMN_NAME_DISABLED_IDS, ids);
    }
    
    public List<String> getDisabledIds(){
        return getList(UserDao.COLUMN_NAME_DISABLED_IDS);
    }
    
    synchronized private void setList(String column, List<String> strList){
        StringBuilder strBuilder = new StringBuilder();
        
        for(String hxid:strList){
            strBuilder.append(hxid).append("$");
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            ContentValues values = new ContentValues();
            values.put(column, strBuilder.toString());

            db.update(UserDao.PREF_TABLE_NAME, values, null,null);
        }
    }
    
    synchronized private List<String> getList(String column){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select " + column + " from " + UserDao.PREF_TABLE_NAME,null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        String strVal = cursor.getString(0);
        if (strVal == null || strVal.equals("")) {
            return null;
        }
        
        cursor.close();
        
        String[] array = strVal.split("$");
        
        if(array.length > 0){
            List<String> list = new ArrayList<String>();
            Collections.addAll(list, array);
            return list;
        }
        
        return null;
    }
    





    synchronized public void closeDB(){
        if(dbHelper != null){
            dbHelper.closeDB();
        }
        dbMgr = null;
    }
    
    


    /**
     * update usernick
     * @param username
     * @param newNick
     */
    public void updateUserNick(String username, String newNick) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.User_COLUMN_NAME_NICK,newNick);
        if (db.isOpen()) {
            db.update(UserDao.User_TABLE_NAME, values, UserDao.User_COLUMN_NAME, new String[]{username});
        }
    }
}
