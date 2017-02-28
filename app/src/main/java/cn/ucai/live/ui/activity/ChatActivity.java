package cn.ucai.live.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import cn.ucai.live.R;

/**
 * Created by wei on 2016/7/27.
 */
public class ChatActivity extends BaseActivity{
  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_chat);

    String username = getIntent().getStringExtra("metUsername");

    getSupportFragmentManager().beginTransaction().add(R.id.root, ChatFragment.newInstance(username, true)).commit();
  }
}
