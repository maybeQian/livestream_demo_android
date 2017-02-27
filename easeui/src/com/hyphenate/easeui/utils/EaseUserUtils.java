package com.hyphenate.easeui.utils;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hyphenate.easeui.R;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.controller.EaseUI.EaseUserProfileProvider;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;

public class EaseUserUtils {
    private static final String TAG = "EaseUserUtils";
    static EaseUserProfileProvider userProvider;
    
    static {
        userProvider = EaseUI.getInstance().getUserProfileProvider();
    }
    
    /**
     * get EaseUser according username
     * @param username
     * @return
     */
    public static EaseUser getUserInfo(String username){
        if(userProvider != null)
            return userProvider.getUser(username);
        
        return null;
    }

    public static User getAppUserInfo(String username){
        if(userProvider != null)
            return userProvider.getAppUser(username);

        return null;
    }

    /**
     * set user avatar
     * @param username
     */
    public static void setUserAvatar(Context context, String username, ImageView imageView){
    	EaseUser user = getUserInfo(username);
        if(user != null && user.getAvatar() != null){
            try {
                int avatarResId = Integer.parseInt(user.getAvatar());
                Glide.with(context).load(avatarResId).into(imageView);
            } catch (Exception e) {
                //use default avatar
                Glide.with(context).load(user.getAvatar()).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.ease_default_avatar).into(imageView);
            }
        }else{
            Glide.with(context).load(R.drawable.ease_default_avatar).into(imageView);
        }
    }

    public static void setAppUserAvatar(Context context, String username, ImageView imageView){
    	User user = getAppUserInfo(username);
        if(user != null && user.getAvatar() != null){
            setAppUserAvatarByPath(context,user.getAvatar(),imageView);
        } else if (username != null) {
            setAppUserAvatarByPath(context,user.getAvatar(),imageView);
        } else {
            Glide.with(context).load(R.drawable.ease_default_avatar).into(imageView);
        }
    }

    public static void setAppUserAvatarByPath(Context context, String path, ImageView imageView) {
        if (path != null) {
            try {
                int avatarResId = Integer.parseInt(path);
                Glide.with(context).load(avatarResId).into(imageView);
            } catch (Exception e) {
                //use default avatar
                Glide.with(context).load(path).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.ease_default_avatar).into(imageView);
            }
        }else{
            Glide.with(context).load(R.drawable.ease_default_avatar).into(imageView);
        }
    }

    public static void setAppGroupAvatar(Context context, String hxid, ImageView imageView) {
        if (hxid != null) {
            try {
                int avatarResId = Integer.parseInt(getGroupAvatarPath(hxid));
                Glide.with(context).load(avatarResId).into(imageView);
            } catch (Exception e) {
                //use default avatar
                Glide.with(context).load(getGroupAvatarPath(hxid)).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.ease_group_icon).into(imageView);
            }
        }else{
            Glide.with(context).load(R.drawable.ease_group_icon).into(imageView);
        }
    }

    private static String getGroupAvatarPath(String hxid) {
        String path = "http://101.251.196.90:8000/SuperWeChatServerV2.0/downloadAvatar?name_or_hxid="
                +hxid+"&avatarType=group_icon&m_avatar_suffix=.jpg";
        return path;
    }

    /**
     * set user's nickname
     */
    public static void setUserNick(String username,TextView textView){
        if(textView != null){
        	EaseUser user = getUserInfo(username);
        	if(user != null && user.getNick() != null){
        		textView.setText(user.getNick());
        	}else{
        		textView.setText(username);
        	}
        }
    }

    public static void setAppUserNick(String username,TextView textView){
        if(textView != null){
        	User user = getAppUserInfo(username);
            if(user != null && user.getMUserNick() != null){
        		textView.setText(user.getMUserNick());
        	}else{
        		textView.setText(username);
        	}
        }
    }



}
