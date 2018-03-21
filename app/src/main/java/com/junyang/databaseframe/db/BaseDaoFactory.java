package com.junyang.databaseframe.db;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 工厂
 */

public class BaseDaoFactory {
    private static final BaseDaoFactory instance = new BaseDaoFactory();
    private SQLiteDatabase mSQLiteDatabase;

    public static BaseDaoFactory getInstance() {
        return instance;
    }

    private BaseDaoFactory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "jett.db");
            mSQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(file, null);
        }
    }

    /**
     * 用来生产basedao对象
     */
    public <T extends BaseDao<M>, M> T getBaseDao(Class<T> daoClass, Class<M> entityClass) {
        BaseDao baseDao = null;
        try {
            baseDao = BaseDao.class.newInstance();
            baseDao.init(mSQLiteDatabase, entityClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T) baseDao;
    }

}
