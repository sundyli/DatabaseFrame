package com.junyang.databaseframe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.junyang.databaseframe.db.BaseDao;
import com.junyang.databaseframe.db.BaseDaoFactory;
import com.junyang.databaseframe.entity.User;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addPermission();
    }

    /**
     * 加入运行时权限，从安卓6.0版本开始需要动态申请
     */
    private void addPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if ( ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void insert(View view) {
        BaseDao baseDao = BaseDaoFactory.getInstance().getBaseDao(BaseDao.class, User.class);
        if (null != baseDao) {
            baseDao.insert(new User("simon", "123"));
            Toast.makeText(this,"执行成功!",Toast.LENGTH_LONG).show();
        }
    }

    public void query(View view) {
        BaseDao baseDao = BaseDaoFactory.getInstance().getBaseDao(BaseDao.class, User.class);
        if (null != baseDao) {
            User user = new User();
            user.setName("simon");
            List<User> list = baseDao.query(user);
            for (int i = 0; i < list.size(); i++) {
                Log.e(TAG, "i:" + i + list.get(i).toString());
            }
            Toast.makeText(this,"执行成功!",Toast.LENGTH_LONG).show();
        }
    }

}
