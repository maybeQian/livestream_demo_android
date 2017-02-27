package cn.ucai.live;

import android.app.Application;

import com.hyphenate.easeui.controller.EaseUI;
import com.ucloud.live.UEasyStreaming;

/**
 * Created by wei on 2016/5/27.
 */
public class LiveApplication extends Application{

  private static LiveApplication instance;

  @Override public void onCreate() {
    super.onCreate();
    instance = this;
    EaseUI.getInstance().init(this, null);
    UEasyStreaming.initStreaming("publish3-key");
  }

  public static LiveApplication getInstance(){
    return instance;
  }
}
