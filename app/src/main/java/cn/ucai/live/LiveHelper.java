package cn.ucai.live;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMError;
import com.hyphenate.EMGroupChangeListener;
import com.hyphenate.EMMessageListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMMessage.ChatType;
import com.hyphenate.chat.EMMessage.Status;
import com.hyphenate.chat.EMMessage.Type;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.controller.EaseUI.EaseEmojiconInfoProvider;
import com.hyphenate.easeui.controller.EaseUI.EaseSettingsProvider;
import com.hyphenate.easeui.controller.EaseUI.EaseUserProfileProvider;
import com.hyphenate.easeui.domain.EaseEmojicon;
import com.hyphenate.easeui.domain.EaseEmojiconGroupEntity;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.model.EaseAtMessageHelper;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.easeui.model.EaseNotifier.EaseNotificationInfoProvider;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.ucai.live.data.NetDao;
import cn.ucai.live.data.local.LiveDBManager;
import cn.ucai.live.data.local.UserDao;
import cn.ucai.live.data.model.Gift;
import cn.ucai.live.data.model.Result;
import cn.ucai.live.ui.activity.ChatActivity;
import cn.ucai.live.ui.activity.LoginActivity;
import cn.ucai.live.ui.activity.MainActivity;
import cn.ucai.live.utils.CommonUtils;
import cn.ucai.live.utils.L;
import cn.ucai.live.utils.OnCompleteListener;
import cn.ucai.live.utils.PreferenceManager;
import cn.ucai.live.utils.ResultUtils;


public class LiveHelper {

    /**
     * data sync listener
     */
    public interface DataSyncListener {
        /**
         * sync complete
         * @param success true：data sync successful，false: failed to sync data
         */
        void onSyncComplete(boolean success);
    }

    protected static final String TAG = "LiveHelper";

	private EaseUI easeUI;

    /**
     * EMEventListener
     */
    protected EMMessageListener messageListener = null;

	private Map<String, EaseUser> contactList;

	private Map<String, User> appContactList;

	private Map<Integer,Gift> appGiftList;


	private static LiveHelper instance = null;

	private LiveModel LiveModel = null;

	/**
     * sync groups status listener
     */
    private List<DataSyncListener> syncGroupsListeners;
    /**
     * sync contacts status listener
     */
    private List<DataSyncListener> syncContactsListeners;
    /**
     * sync blacklist status listener
     */
    private List<DataSyncListener> syncBlackListListeners;

    private boolean isSyncingGroupsWithServer = false;
    private boolean isSyncingContactsWithServer = false;
    private boolean isSyncingBlackListWithServer = false;
    private boolean isGroupsSyncedWithServer = false;
    private boolean isContactsSyncedWithServer = false;
    private boolean isBlackListSyncedWithServer = false;


	private String username;

    private Context appContext;


    private UserDao userDao;

    private LocalBroadcastManager broadcastManager;

    private boolean isGroupAndContactListenerRegisted;

	private LiveHelper() {
	}

	public synchronized static LiveHelper getInstance() {
		if (instance == null) {
			instance = new LiveHelper();
		}
		return instance;
	}

	/**
	 * init helper
	 *
	 * @param context
	 *            application context
	 */
	public void init(Context context) {
	    LiveModel = new LiveModel(context);
	    EMOptions options = initChatOptions();
	    //use default options if options is null
		if (EaseUI.getInstance().init(context, options)) {
		    appContext = context;

		    //debug mode, you'd better set it to false, if you want release your App officially.
		    EMClient.getInstance().setDebugMode(false);
		    //get easeui instance
		    easeUI = EaseUI.getInstance();
		    //to set user's profile and avatar
		    setEaseUIProviders();
			//initialize preference manager
			PreferenceManager.init(context);



            // resolution
            String resolution = PreferenceManager.getInstance().getCallBackCameraResolution();
            if (resolution.equals("")) {
                resolution = PreferenceManager.getInstance().getCallFrontCameraResolution();
            }

            setGlobalListeners();
			broadcastManager = LocalBroadcastManager.getInstance(appContext);
	        initDbDao();

            initGiftList();
		}
	}

    private void initGiftList() {
        NetDao.loadAllGifts(appContext, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                boolean success=false;
                if (s != null) {
                    Result result = ResultUtils.getListResultFromJson(s, Gift.class);
                    if (result != null && result.isRetMsg()) {
                        List<Gift> list= (List<Gift>) result.getRetData();
                        if (list != null && list.size() > 0) {
                            success=true;
                            Map<Integer, Gift> map = new HashMap<Integer, Gift>();
                            for (Gift gift : list) {
                                map.put(gift.getId(), gift);
                            }
                            getAppGiftList().clear();
                            getAppGiftList().putAll(map);

                            UserDao dao = new UserDao(appContext);
                            dao.saveAppGiftList(list);
                        }
                    }
                }
                if (!success) {
                    L.e(TAG,"initGiftList fail");
                }
            }

            @Override
            public void onError(String error) {
                L.e(TAG,"initGiftList fail,error="+error);
            }
        });
    }


    private EMOptions initChatOptions(){
        Log.d(TAG, "init HuanXin Options");

        EMOptions options = new EMOptions();
        // set if accept the invitation automatically
        options.setAcceptInvitationAlways(false);
        // set if you need read ack
        options.setRequireAck(true);
        // set if you need delivery ack
        options.setRequireDeliveryAck(false);

        //you need apply & set your own id if you want to use google cloud messaging.
        options.setGCMNumber("324169311137");
        //you need apply & set your own id if you want to use Mi push notification
        options.setMipushConfig("2882303761517426801", "5381742660801");
        //you need apply & set your own id if you want to use Huawei push notification
        options.setHuaweiPushAppId("10492024");


        if (LiveModel.isCustomAppkeyEnabled() && LiveModel.getCutomAppkey() != null && !LiveModel.getCutomAppkey().isEmpty()) {
            options.setAppKey(LiveModel.getCutomAppkey());
        }

        options.allowChatroomOwnerLeave(getModel().isChatroomOwnerLeaveAllowed());
        options.setDeleteMessagesAsExitGroup(getModel().isDeleteMessagesAsExitGroup());
        options.setAutoAcceptGroupInvitation(getModel().isAutoAcceptGroupInvitation());

        return options;
    }

    protected void setEaseUIProviders() {
    	// set profile provider if you want easeUI to handle avatar and nickname
        easeUI.setUserProfileProvider(new EaseUserProfileProvider() {

            @Override
            public EaseUser getUser(String username) {
                return getUserInfo(username);
            }

            @Override
            public User getAppUser(String username) {
                return getAppUserInfo(username);
            }
        });

        //set options
        easeUI.setSettingsProvider(new EaseSettingsProvider() {

            @Override
            public boolean isSpeakerOpened() {
                return LiveModel.getSettingMsgSpeaker();
            }

            @Override
            public boolean isMsgVibrateAllowed(EMMessage message) {
                return LiveModel.getSettingMsgVibrate();
            }

            @Override
            public boolean isMsgSoundAllowed(EMMessage message) {
                return LiveModel.getSettingMsgSound();
            }

            @Override
            public boolean isMsgNotifyAllowed(EMMessage message) {
                if(message == null){
                    return LiveModel.getSettingMsgNotification();
                }
                if(!LiveModel.getSettingMsgNotification()){
                    return false;
                }else{
                    String chatUsename = null;
                    List<String> notNotifyIds = null;
                    // get user or group id which was blocked to show message notifications
                    if (message.getChatType() == ChatType.Chat) {
                        chatUsename = message.getFrom();
                        notNotifyIds = LiveModel.getDisabledIds();
                    } else {
                        chatUsename = message.getTo();
                        notNotifyIds = LiveModel.getDisabledGroups();
                    }

                    if (notNotifyIds == null || !notNotifyIds.contains(chatUsename)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        });
        //set emoji icon provider
        easeUI.setEmojiconInfoProvider(new EaseEmojiconInfoProvider() {

            @Override
            public EaseEmojicon getEmojiconInfo(String emojiconIdentityCode) {
                return null;
            }

            @Override
            public Map<String, Object> getTextEmojiconMapping() {
                return null;
            }
        });

        //set notification options, will use default if you don't set it
        easeUI.getNotifier().setNotificationInfoProvider(new EaseNotificationInfoProvider() {

            @Override
            public String getTitle(EMMessage message) {
              //you can update title here
                return null;
            }

            @Override
            public int getSmallIcon(EMMessage message) {
              //you can update icon here
                return 0;
            }

            @Override
            public String getDisplayedText(EMMessage message) {
            	// be used on notification bar, different text according the message type.
                String ticker = EaseCommonUtils.getMessageDigest(message, appContext);
                if(message.getType() == Type.TXT){
                    ticker = ticker.replaceAll("\\[.{2,3}\\]", "[表情]");
                }
                EaseUser user = getUserInfo(message.getFrom());
                if(user != null){
                    if(EaseAtMessageHelper.get().isAtMeMsg(message)){
                        return String.format(appContext.getString(R.string.at_your_in_group), user.getNick());
                    }
                    return user.getNick() + ": " + ticker;
                }else{
                    if(EaseAtMessageHelper.get().isAtMeMsg(message)){
                        return String.format(appContext.getString(R.string.at_your_in_group), message.getFrom());
                    }
                    return message.getFrom() + ": " + ticker;
                }
            }

            @Override
            public String getLatestText(EMMessage message, int fromUsersNum, int messageNum) {
                // here you can customize the text.
                // return fromUsersNum + "contacts send " + messageNum + "messages to you";
            	return null;
            }

            @Override
            public Intent getLaunchIntent(EMMessage message) {
            	// you can set what activity you want display when user click the notification
                Intent intent = new Intent(appContext, ChatActivity.class);
                    ChatType chatType = message.getChatType();
                    if (chatType == ChatType.Chat) { // single chat message
                        intent.putExtra("userId", message.getFrom());
                        intent.putExtra("chatType", EaseConstant.CHATTYPE_SINGLE);
                    } else { // group chat message
                        // message.getTo() is the group id
                        intent.putExtra("userId", message.getTo());
                        if(chatType == ChatType.GroupChat){
                            intent.putExtra("chatType", EaseConstant.CHATTYPE_GROUP);
                        }else{
                            intent.putExtra("chatType", EaseConstant.CHATTYPE_CHATROOM);
                        }

                    }

                return intent;
            }
        });
    }

    EMConnectionListener connectionListener;
    /**
     * set global listener
     */
    protected void setGlobalListeners(){
        syncGroupsListeners = new ArrayList<DataSyncListener>();
        syncContactsListeners = new ArrayList<DataSyncListener>();
        syncBlackListListeners = new ArrayList<DataSyncListener>();

        isGroupsSyncedWithServer = LiveModel.isGroupsSynced();
        isContactsSyncedWithServer = LiveModel.isContactSynced();
        isBlackListSyncedWithServer = LiveModel.isBacklistSynced();

        // create the global connection listener
        connectionListener = new EMConnectionListener() {
            @Override
            public void onDisconnected(int error) {
                EMLog.d("global listener", "onDisconnect" + error);
                if (error == EMError.USER_REMOVED) {
                    onUserException(LiveConstants.ACCOUNT_REMOVED);
                } else if (error == EMError.USER_LOGIN_ANOTHER_DEVICE) {
                    onUserException(LiveConstants.ACCOUNT_CONFLICT);
                } else if (error == EMError.SERVER_UNKNOWN_ERROR) {
                    onUserException(LiveConstants.ACCOUNT_FORBIDDEN);
                }
            }

            @Override
            public void onConnected() {
                // in case group and contact were already synced, we supposed to notify sdk we are ready to receive the events
                if (isGroupsSyncedWithServer && isContactsSyncedWithServer) {
                    EMLog.d(TAG, "group and contact already synced with servre");
                } else {
                    if (!isGroupsSyncedWithServer) {
                        asyncFetchGroupsFromServer(null);
                    }

                    if (!isContactsSyncedWithServer) {
                        asyncFetchContactsFromServer(null);
                    }

                    if (!isBlackListSyncedWithServer) {
                        asyncFetchBlackListFromServer(null);
                    }
                }
            }
        };
        //register connection listener
        EMClient.getInstance().addConnectionListener(connectionListener);
        //register group and contact event listener
        registerGroupAndContactListener();
        //register message event listener
        registerMessageListener();

    }

    private void initDbDao() {
        userDao = new UserDao(appContext);
    }

    /**
     * register group and contact listener, you need register when login
     */
    public void registerGroupAndContactListener(){
        if(!isGroupAndContactListenerRegisted){
            EMClient.getInstance().groupManager().addGroupChangeListener(new MyGroupChangeListener());
            EMClient.getInstance().contactManager().setContactListener(new MyContactListener());
            isGroupAndContactListenerRegisted = true;
        }

    }

    /**
     * group change listener
     */
    class MyGroupChangeListener implements EMGroupChangeListener {
        @Override
        public void onInvitationReceived(String s, String s1, String s2, String s3) {

        }

        @Override
        public void onApplicationReceived(String s, String s1, String s2, String s3) {

        }

        @Override
        public void onApplicationAccept(String s, String s1, String s2) {

        }

        @Override
        public void onApplicationDeclined(String s, String s1, String s2, String s3) {

        }

        @Override
        public void onInvitationAccpted(String s, String s1, String s2) {

        }

        @Override
        public void onInvitationDeclined(String s, String s1, String s2) {

        }

        @Override
        public void onUserRemoved(String s, String s1) {

        }

        @Override
        public void onGroupDestroy(String s, String s1) {

        }

        @Override
        public void onAutoAcceptInvitationFromGroup(String s, String s1, String s2) {

        }
    }
    
    /***
     * 好友变化listener
     * 
     */
    public class MyContactListener implements EMContactListener {
        @Override
        public void onContactAdded(String s) {

        }

        @Override
        public void onContactDeleted(String s) {

        }

        @Override
        public void onContactInvited(String s, String s1) {

        }

        @Override
        public void onContactAgreed(String s) {

        }

        @Override
        public void onContactRefused(String s) {

        }
    }

    /**
     * user met some exception: conflict, removed or forbidden
     */
    protected void onUserException(String exception){
        EMLog.e(TAG, "onUserException: " + exception);
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(exception, true);
        appContext.startActivity(intent);
    }
 
	private EaseUser getUserInfo(String username){
		// To get instance of EaseUser, here we get it from the user list in memory
		// You'd better cache it if you get it from your server
        EaseUser user = null;
//        if(username.equals(EMClient.getInstance().getCurrentUser()))
//            return getUserProfileManager().getCurrentUserInfo();
        user = getContactList().get(username);


        // if user is not in your contacts, set inital letter for him/her
        if(user == null){
            user = new EaseUser(username);
            EaseCommonUtils.setUserInitialLetter(user);
        }
        return user;
	}


	private User getAppUserInfo(String username){
		// To get instance of EaseUser, here we get it from the user list in memory
		// You'd better cache it if you get it from your server
        User user = null;
        user=getAppContactList().get(username);
        // if user is not in your contacts, set inital letter for him/her
        if(user == null){
            user = new User(username);
            EaseCommonUtils.setUserInitialLetter(user);
        }
        return user;
	}

	 /**
     * Global listener
     * If this event already handled by an activity, you don't need handle it again
     * activityList.size() <= 0 means all activities already in background or not in Activity Stack
     */
    protected void registerMessageListener() {
    	messageListener = new EMMessageListener() {
            private BroadcastReceiver broadCastReceiver = null;

			@Override
			public void onMessageReceived(List<EMMessage> messages) {
			    for (EMMessage message : messages) {
			        EMLog.d(TAG, "onMessageReceived id : " + message.getMsgId());
			        // in background, do not refresh UI, notify it in notification bar
			        if(!easeUI.hasForegroundActivies()){
			            getNotifier().onNewMsg(message);
			        }
			    }
			}
			
			@Override
			public void onCmdMessageReceived(List<EMMessage> messages) {
			    for (EMMessage message : messages) {
                    EMLog.d(TAG, "receive command message");
                    //get message body
                    EMCmdMessageBody cmdMsgBody = (EMCmdMessageBody) message.getBody();
                    final String action = cmdMsgBody.action();//获取自定义action
                    //red packet code : 处理红包回执透传消息
//                    if(!easeUI.hasForegroundActivies()){
//                        if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)){
//                            RedPacketUtil.receiveRedPacketAckMessage(message);
//                            broadcastManager.sendBroadcast(new Intent(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION));
//                        }
//                    }

                    if (action.equals("__Call_ReqP2P_ConferencePattern")) {
                        String title = message.getStringAttribute("em_apns_ext", "conference call");
                        Toast.makeText(appContext, title, Toast.LENGTH_LONG).show();
                    }
                    //end of red packet code
                    //获取扩展属性 此处省略
                    //maybe you need get extension of your message
                    //message.getStringAttribute("");
                    EMLog.d(TAG, String.format("Command：action:%s,message:%s", action,message.toString()));
                }
			}

            @Override
            public void onMessageReadAckReceived(List<EMMessage> list) {

            }

            @Override
            public void onMessageDeliveryAckReceived(List<EMMessage> list) {

            }


			@Override
			public void onMessageChanged(EMMessage message, Object change) {
				
			}
		};
		
        EMClient.getInstance().chatManager().addMessageListener(messageListener);
    }

	/**
	 * if ever logged in
	 * 
	 * @return
	 */
	public boolean isLoggedIn() {
		return EMClient.getInstance().isLoggedInBefore();
	}

	/**
	 * logout
	 * 
	 * @param unbindDeviceToken
	 *            whether you need unbind your device token
	 * @param callback
	 *            callback
	 */
	public void logout(boolean unbindDeviceToken, final EMCallBack callback) {
		endCall();
		Log.d(TAG, "logout: " + unbindDeviceToken);
		EMClient.getInstance().logout(unbindDeviceToken, new EMCallBack() {

			@Override
			public void onSuccess() {
				Log.d(TAG, "logout: onSuccess");
			    reset();
				if (callback != null) {
					callback.onSuccess();
				}

			}

			@Override
			public void onProgress(int progress, String status) {
				if (callback != null) {
					callback.onProgress(progress, status);
				}
			}

			@Override
			public void onError(int code, String error) {
				Log.d(TAG, "logout: onSuccess");
                reset();
				if (callback != null) {
					callback.onError(code, error);
				}
			}
		});
	}
	
	/**
	 * get instance of EaseNotifier
	 * @return
	 */
	public EaseNotifier getNotifier(){
	    return easeUI.getNotifier();
	}
	
	public LiveModel getModel(){
        return (LiveModel) LiveModel;
    }
	
	/**
	 * update contact list
	 * 
	 * @param aContactList
	 */
	public void setContactList(Map<String, EaseUser> aContactList) {
		if(aContactList == null){
		    if (contactList != null) {
		        contactList.clear();
		    }
			return;
		}
		
		contactList = aContactList;
	}
	
	/**
     * save single contact 
     */
    public void saveContact(EaseUser user){
    	contactList.put(user.getUsername(), user);
    	LiveModel.saveContact(user);
    }
    
    /**
     * get contact list
     *
     * @return
     */
    public Map<String, EaseUser> getContactList() {
        if (isLoggedIn() && contactList == null) {
            contactList = LiveModel.getContactList();
        }
        
        // return a empty non-null object to avoid app crash
        if(contactList == null){
        	return new Hashtable<String, EaseUser>();
        }
        
        return contactList;
    }
	/**
	 * update contact list
	 *
	 * @param aContactList
	 */
	public void setAppContactList(Map<String, User> aContactList) {
		if(aContactList == null){
		    if (appContactList != null) {
		        appContactList.clear();
		    }
			return;
		}

		appContactList = aContactList;
	}

    /**
     *set gift list
     * @param list
     */
	public void setAppGiftList(Map<Integer,Gift> list) {
		if(list == null){
		    if (appGiftList != null) {
                appGiftList.clear();
		    }
			return;
		}

        appGiftList = list;
	}

	/**
     * save single contact
     */
    public void saveAppContact(User user){
    	getAppContactList().put(user.getMUserName(), user);
    	LiveModel.saveAppContact(user);
    }

    /**
     * get contact list
     *
     * @return
     */
    public Map<String, User> getAppContactList() {
        if (isLoggedIn() && appContactList == null) {
            appContactList = LiveModel.getAppContactList();
        }

        // return a empty non-null object to avoid app crash
        if(appContactList == null){
        	return new Hashtable<String, User>();
        }

        return appContactList;
    }

    /**
     * get gift list
     *
     * @return
     */
    public Map<Integer,Gift> getAppGiftList() {
        if (appGiftList == null || appGiftList.size()==0) {
            appGiftList = LiveModel.getAppGiftList();
        }

        // return a empty non-null object to avoid app crash
        if(appGiftList == null){
        	return new Hashtable<Integer, Gift>();
        }

        return appGiftList;
    }

    /**
     * set current username
     * @param username
     */
    public void setCurrentUserName(String username){
    	this.username = username;
    	LiveModel.setCurrentUserName(username);
    }
    
    /**
     * get current user's id
     */
    public String getCurrentUsernName(){
    	if(username == null){
    		username = LiveModel.getCurrentUsernName();
    	}
    	return username;
    }


	 /**
     * update user list to cache and database
     *
     * @param contactInfoList
     */
    public void updateContactList(List<EaseUser> contactInfoList) {
         for (EaseUser u : contactInfoList) {
            contactList.put(u.getUsername(), u);
         }
         ArrayList<EaseUser> mList = new ArrayList<EaseUser>();
         mList.addAll(contactList.values());
         LiveModel.saveContactList(mList);
    }

    public void updateAppContactList(List<User> contactInfoList) {
         for (User u : contactInfoList) {
            appContactList.put(u.getMUserName(), u);
         }
         ArrayList<User> mList = new ArrayList<>();
         mList.addAll(appContactList.values());
         LiveModel.saveAppContactList(mList);
    }


	void endCall() {
		try {
			EMClient.getInstance().callManager().endCall();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
  public void addSyncGroupListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (!syncGroupsListeners.contains(listener)) {
            syncGroupsListeners.add(listener);
        }
    }

    public void removeSyncGroupListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (syncGroupsListeners.contains(listener)) {
            syncGroupsListeners.remove(listener);
        }
    }

    public void addSyncContactListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (!syncContactsListeners.contains(listener)) {
            syncContactsListeners.add(listener);
        }
    }

    public void removeSyncContactListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (syncContactsListeners.contains(listener)) {
            syncContactsListeners.remove(listener);
        }
    }

    public void addSyncBlackListListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (!syncBlackListListeners.contains(listener)) {
            syncBlackListListeners.add(listener);
        }
    }

    public void removeSyncBlackListListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (syncBlackListListeners.contains(listener)) {
            syncBlackListListeners.remove(listener);
        }
    }
	
	/**
    * Get group list from server
    * This method will save the sync state
    * @throws HyphenateException
    */
   public synchronized void asyncFetchGroupsFromServer(final EMCallBack callback){
       if(isSyncingGroupsWithServer){
           return;
       }
       
       isSyncingGroupsWithServer = true;
       
       new Thread(){
           @Override
           public void run(){
               try {
                   EMClient.getInstance().groupManager().getJoinedGroupsFromServer();
                   
                   // in case that logout already before server returns, we should return immediately
                   if(!isLoggedIn()){
                       isGroupsSyncedWithServer = false;
                       isSyncingGroupsWithServer = false;
                       noitifyGroupSyncListeners(false);
                       return;
                   }
                   
                   LiveModel.setGroupsSynced(true);
                   
                   isGroupsSyncedWithServer = true;
                   isSyncingGroupsWithServer = false;
                   
                   //notify sync group list success
                   noitifyGroupSyncListeners(true);

                   if(callback != null){
                       callback.onSuccess();
                   }
               } catch (HyphenateException e) {
                   LiveModel.setGroupsSynced(false);
                   isGroupsSyncedWithServer = false;
                   isSyncingGroupsWithServer = false;
                   noitifyGroupSyncListeners(false);
                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }
           
           }
       }.start();
   }

   public void noitifyGroupSyncListeners(boolean success){
       for (DataSyncListener listener : syncGroupsListeners) {
           listener.onSyncComplete(success);
       }
   }
   
   public void asyncFetchContactsFromServer(final EMValueCallBack<List<String>> callback){
       if(isSyncingContactsWithServer){
           return;
       }
       
       isSyncingContactsWithServer = true;
//       if (isLoggedIn()) {
//           NetDao.loadContactList(appContext, EMClient.getInstance().getCurrentUser(), new OnCompleteListener<String>() {
//               @Override
//               public void onSuccess(String s) {
//                   if (s != null) {
//                       Result result = ResultUtils.getListResultFromJson(s, User.class);
//                       if (result != null && result.isRetMsg()) {
//                           List<User> list = (List<User>) result.getRetData();
//                           Map<String, User> userMap = new HashMap<String, User>();
//                           for (User u : list) {
//                               EaseCommonUtils.setUserInitialLetter(u);
//                               userMap.put(u.getMUserName(), u);
//                           }
//                           // save the contact list to cache
//                           getAppContactList().clear();
//                           getAppContactList().putAll(userMap);
//                           // save the contact list to database
//                           UserDao dao = new UserDao(appContext);
//                           dao.saveAppContactList(list);
//                       }
//                   }
//               }
//
//               @Override
//               public void onError(String error) {
//
//               }
//           });
//       }
       new Thread(){
           @Override
           public void run(){
               List<String> usernames = null;
               try {
                   usernames = EMClient.getInstance().contactManager().getAllContactsFromServer();
                   // in case that logout already before server returns, we should return immediately
                   if(!isLoggedIn()){
                       isContactsSyncedWithServer = false;
                       isSyncingContactsWithServer = false;
                       notifyContactsSyncListener(false);
                       return;
                   }

                   Map<String, EaseUser> userlist = new HashMap<String, EaseUser>();
                   for (String username : usernames) {
                       EaseUser user = new EaseUser(username);
                       EaseCommonUtils.setUserInitialLetter(user);
                       userlist.put(username, user);
                   }
                   // save the contact list to cache
                   getContactList().clear();
                   getContactList().putAll(userlist);
                    // save the contact list to database
                   UserDao dao = new UserDao(appContext);
                   List<EaseUser> users = new ArrayList<EaseUser>(userlist.values());
                   dao.saveContactList(users);

                   LiveModel.setContactSynced(true);
                   EMLog.d(TAG, "set contact syn status to true");
                   
                   isContactsSyncedWithServer = true;
                   isSyncingContactsWithServer = false;
                   
                   //notify sync success
                   notifyContactsSyncListener(true);
                   
//                   getUserProfileManager().asyncFetchContactInfosFromServer(usernames,new EMValueCallBack<List<EaseUser>>() {
//
//                       @Override
//                       public void onSuccess(List<EaseUser> uList) {
//                           updateContactList(uList);
//                           getUserProfileManager().notifyContactInfosSyncListener(true);
//                       }
//
//                       @Override
//                       public void onError(int error, String errorMsg) {
//                       }
//                   });
                   if(callback != null){
                       callback.onSuccess(usernames);
                   }
               } catch (HyphenateException e) {
                   LiveModel.setContactSynced(false);
                   isContactsSyncedWithServer = false;
                   isSyncingContactsWithServer = false;
                   notifyContactsSyncListener(false);
                   e.printStackTrace();
                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }
               
           }
       }.start();
   }

   public void notifyContactsSyncListener(boolean success){
       for (DataSyncListener listener : syncContactsListeners) {
           listener.onSyncComplete(success);
       }
   }
   
   public void asyncFetchBlackListFromServer(final EMValueCallBack<List<String>> callback){
       
       if(isSyncingBlackListWithServer){
           return;
       }
       
       isSyncingBlackListWithServer = true;
       
       new Thread(){
           @Override
           public void run(){
               try {
                   List<String> usernames = EMClient.getInstance().contactManager().getBlackListFromServer();
                   
                   // in case that logout already before server returns, we should return immediately
                   if(!isLoggedIn()){
                       isBlackListSyncedWithServer = false;
                       isSyncingBlackListWithServer = false;
                       notifyBlackListSyncListener(false);
                       return;
                   }
                   
                   LiveModel.setBlacklistSynced(true);
                   
                   isBlackListSyncedWithServer = true;
                   isSyncingBlackListWithServer = false;
                   
                   notifyBlackListSyncListener(true);
                   if(callback != null){
                       callback.onSuccess(usernames);
                   }
               } catch (HyphenateException e) {
                   LiveModel.setBlacklistSynced(false);
                   
                   isBlackListSyncedWithServer = false;
                   isSyncingBlackListWithServer = true;
                   e.printStackTrace();
                   
                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }
               
           }
       }.start();
   }
	
	public void notifyBlackListSyncListener(boolean success){
        for (DataSyncListener listener : syncBlackListListeners) {
            listener.onSyncComplete(success);
        }
    }
    
    public boolean isSyncingGroupsWithServer() {
        return isSyncingGroupsWithServer;
    }

    public boolean isSyncingContactsWithServer() {
        return isSyncingContactsWithServer;
    }

    public boolean isSyncingBlackListWithServer() {
        return isSyncingBlackListWithServer;
    }
    
    public boolean isGroupsSyncedWithServer() {
        return isGroupsSyncedWithServer;
    }

    public boolean isContactsSyncedWithServer() {
        return isContactsSyncedWithServer;
    }

    public boolean isBlackListSyncedWithServer() {
        return isBlackListSyncedWithServer;
    }
	
    synchronized void reset(){
        isSyncingGroupsWithServer = false;
        isSyncingContactsWithServer = false;
        isSyncingBlackListWithServer = false;
        
        LiveModel.setGroupsSynced(false);
        LiveModel.setContactSynced(false);
        LiveModel.setBlacklistSynced(false);
        
        isGroupsSyncedWithServer = false;
        isContactsSyncedWithServer = false;
        isBlackListSyncedWithServer = false;

        isGroupAndContactListenerRegisted = false;
        
        setContactList(null);
        setAppContactList(null);
        LiveDBManager.getInstance().closeDB();
    }

    public void pushActivity(Activity activity) {
        easeUI.pushActivity(activity);
    }

    public void popActivity(Activity activity) {
        easeUI.popActivity(activity);
    }

    public void asyncGetCurrentUserInfo(Activity activity) {
        NetDao.getUserInfoByUsername(activity, EMClient.getInstance().getCurrentUser(), new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (s != null) {
                    L.e(TAG,"asyncGetCurrentUserInfo,s="+s);
                    Result result = ResultUtils.getResultFromJson(s, User.class);
                    if (result != null) {
                        if (result.isRetMsg()) {
                            L.e(TAG,"asyncGetCurrentUserInfo,result="+result);
                            User user= (User) result.getRetData();
                            if (user != null) {
                                //save user info to db
                                LiveHelper.getInstance().saveAppContact(user);
                                PreferenceManager.getInstance().setCurrentUserNick(user.getMUserNick());
                                PreferenceManager.getInstance().setCurrentUserAvatar(user.getAvatar());
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
                L.e(TAG,"asyncGetCurrentUserInfo,error="+error);
            }
        });
    }
}
