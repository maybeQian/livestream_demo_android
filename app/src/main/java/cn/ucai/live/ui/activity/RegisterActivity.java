package cn.ucai.live.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.live.I;
import cn.ucai.live.LiveHelper;
import cn.ucai.live.R;
import cn.ucai.live.data.NetDao;
import cn.ucai.live.data.model.Result;
import cn.ucai.live.utils.CommonUtils;
import cn.ucai.live.utils.L;
import cn.ucai.live.utils.MD5;
import cn.ucai.live.utils.MFGT;
import cn.ucai.live.utils.OnCompleteListener;
import cn.ucai.live.utils.ResultUtils;

public class RegisterActivity extends BaseActivity {

    @BindView(R.id.email)
    EditText metUsername;
    @BindView(R.id.password)
    EditText metPassword;
    @BindView(R.id.register)
    Button register;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.etNickname)
    EditText metNickname;
    @BindView(R.id.password_confirm)
    EditText metConfirmPassword;

    String username,usernick,pwd;
    ProgressDialog pd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = metUsername.getText().toString().trim();
                usernick = metNickname.getText().toString().trim();
                pwd = metPassword.getText().toString().trim();
                String confirm_pwd = metConfirmPassword.getText().toString().trim();
                if (TextUtils.isEmpty(username)) {
                    CommonUtils.showShortToast(R.string.User_name_cannot_be_empty);
                    metUsername.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(usernick)) {
                    CommonUtils.showShortToast(R.string.User_nick_cannot_be_empty);
                    metNickname.requestFocus();
                    return;
                }else if (TextUtils.isEmpty(pwd)) {
                    CommonUtils.showShortToast(R.string.Password_cannot_be_empty);
                    metPassword.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(confirm_pwd)) {
                    CommonUtils.showShortToast(R.string.Confirm_password_cannot_be_empty);
                    metConfirmPassword.requestFocus();
                    return;
                } else if (!pwd.equals(confirm_pwd)) {
                    CommonUtils.showShortToast(R.string.Two_input_password);
                    return;
                }

                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
                    pd = new ProgressDialog(RegisterActivity.this);
                    pd.setMessage(getResources().getString(R.string.Is_the_registered));
                    pd.show();
                    //注册自己的服务器的账号
                    registerAppServer();

                }


            }
        });

    }
    private void registerAppServer() {
        NetDao.register(this, username, usernick, pwd, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (s != null) {
                    Result result = ResultUtils.getResultFromJson(s, null);
                    if (result != null) {
                        if (result.isRetMsg()) {
                            //注册成功后调用环信的注册
                            registerEMServer();
                        } else {
                            pd.dismiss();
                            if (result.getRetCode() == I.MSG_REGISTER_USERNAME_EXISTS) {
                                CommonUtils.showShortToast(R.string.User_already_exists);
                            } else {
                                CommonUtils.showShortToast(R.string.Registration_failed);
                            }
                        }
                    } else {
                        pd.dismiss();
                        CommonUtils.showShortToast(R.string.Registration_failed);
                    }
                } else {
                    pd.dismiss();
                    CommonUtils.showShortToast(R.string.Registration_failed);
                }
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                CommonUtils.showShortToast(R.string.Registration_failed);
            }
        });
    }
    private void registerEMServer() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // call method in SDK
                    EMClient.getInstance().createAccount(username, MD5.getMessageDigest(pwd));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            // save current user
                            LiveHelper.getInstance().setCurrentUserName(username);
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();
                            MFGT.gotoLogin(RegisterActivity.this);
                            finish();
                        }
                    });
                } catch (final HyphenateException e) {
                    //环信注册失败后删除相关信息
                    unRegisterServer();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            int errorCode = e.getErrorCode();
                            if (errorCode == EMError.NETWORK_ERROR) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).start();
    }
    private void unRegisterServer() {
        NetDao.unRegister(this, username, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                L.e("RegisterActivity","result="+result);
            }

            @Override
            public void onError(String error) {
                L.e("RegisterActivity","error="+error);
            }
        });
    }

}
