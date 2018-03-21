package com.junyang.databaseframe.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.junyang.databaseframe.annotation.DbField;
import com.junyang.databaseframe.annotation.DbTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据库操作基类
 */

public class BaseDao<T> implements IBaseDao<T> {
    // 持有数据库操作的引用
    private SQLiteDatabase mSQLiteDatabase;
    // 表名
    private String mTableName;
    // 持有操作数据库所对应的java类型
    private Class<T> mEntityClass;
    // 初始化标记
    private boolean initialized = false;
    // 定义一个缓存空间(key-字段名, value-成员变量)
    private HashMap<String, Field> mCacheMap;

//    private BaseDao() {
//    }

    // 框架内部的逻辑，最好不要提供构造方法给调用层用
    protected boolean init(SQLiteDatabase sqLiteDatabase, Class<T> entityClass) {
        mSQLiteDatabase = sqLiteDatabase;
        mEntityClass = entityClass;
        // 根据传入的entityClass类型来建立表,只需要建一次
        if (!initialized) {
            // 取到表名
            if (null == entityClass.getAnnotation(DbTable.class)) {
                // 反射获取类名
                mTableName = entityClass.getSimpleName();
            } else {
                // 取注解上的名字
                mTableName = entityClass.getAnnotation(DbTable.class).value();
            }
            if (!mSQLiteDatabase.isOpen()) {
                return false;
            }
            // 执行建表操作
            String createTableSql = getCreateTableSql();
            sqLiteDatabase.execSQL(createTableSql);
            mCacheMap = new HashMap<>();
            initCacheMap();
            initialized = true;
        }

        return true;
    }

    @Override
    public long insert(T entity) {
        // 准备好ContentValues中需要的数据,（key--字段名，value--值）
        Map<String, String> map = getValues(entity);
        // 把数据转移到ContentValues中
        ContentValues contentValues = getContentValues(map);

        return mSQLiteDatabase.insert(mTableName, null, contentValues);
    }

    @Override
    public long update(T entity, T where) {
        // 准备好ContentValues中需要的数据,（key--字段名，value--值）
        Map<String, String> values = getValues(entity);
        // 把数据转移到ContentValues中
        ContentValues contentValues = getContentValues(values);
        Map<String, String> whereClause = getValues(where);
        Condition condition = new Condition(whereClause);

        return mSQLiteDatabase.update(mTableName, contentValues, condition.whereClasue, condition.whereArgs);
    }

    @Override
    public int delete(T where) {
        // 准备好ContentValues中需要的数据,（key--字段名，value--值）
        Map<String, String> map = getValues(where);
        Condition condition = new Condition(map);

        return mSQLiteDatabase.delete(mTableName, condition.whereClasue, condition.whereArgs);
    }

    @Override
    public List<T> query(T where) {
        return query(where, null, null, null);
    }

    @Override
    public List<T> query(T where, String orderBy, Integer startIndex, Integer limit) {
        // 准备好ContentValues中需要的数据,（key--字段名，value--值）
        Map<String, String> map = getValues(where);
        String limitString = null;
        if (startIndex != null && limit != null) {
            limitString = startIndex + " , " + limit;
        }
        Condition condition = new Condition(map);
        Cursor cursor = mSQLiteDatabase.query(mTableName, null, condition.whereClasue,
                condition.whereArgs, null, null, orderBy, limitString);
        //定义一个用来解析游标的方法
        List<T> result = getResult(cursor, where);
        return result;
    }

    /**
     *
     * @return 获取创建表的SQL语句字符串
     */
    private String getCreateTableSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("create table if not exists ");
        sb.append(mTableName);
        sb.append("(");
        // 反射得到所有的成员变量
        Field[] fields = mEntityClass.getDeclaredFields();
        for (Field field : fields) {
            // 获取成员变量的类型
            Class type = field.getType();
            String fieldName;
            if (field.getAnnotation(DbField.class) != null) {
                fieldName = field.getAnnotation(DbField.class).value();
            } else {
                fieldName = field.getName();
            }
            if (fieldName.equals("$change") || fieldName.equals("serialVersionUID")) {
                continue;
            }
            sb.append(fieldName);
            if (type == String.class) {
                sb.append(" TEXT");
            } else if (type == Integer.class) {
                sb.append(" INTEGER");
            } else if (type == Long.class) {
                sb.append(" BIGINT");
            } else if (type == Double.class) {
                sb.append(" DOUBLE");
            } else if (type == byte[].class) {
                sb.append(" BLOB");
            } else {
                // 不支持的类型号
                continue;
            }
            if (fieldName.equals("id")) {
                sb.append(" PRIMARY KEY AUTOINCREMENT NOT NULL,");
            } else {
                sb.append(",");
            }
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");

        return sb.toString();
    }

    /**
     * 换成字段及对应的值
     */
    private void initCacheMap() {
        // 1.取到所有的列名
        String sql = "select * from " + mTableName + " limit 1,0"; // 空表
        Cursor cursor = mSQLiteDatabase.rawQuery(sql, null);
        if (cursor == null) {
            return;
        }
        String[] columnNames = cursor.getColumnNames();
        cursor.close();
        // 2.取所有的成员变量
        Field[] fields = mEntityClass.getDeclaredFields();
        // 把所有成员变量的访问权限打开
        for (Field field : fields) {
            field.setAccessible(true);
        }
        // 对1和2进行映射
        for (String columnName : columnNames) {
            Field columnField = null;
            for (Field field : fields) {
                String fieldName;
                if (field.getAnnotation(DbField.class) != null) {
                    fieldName = field.getAnnotation(DbField.class).value();
                } else {
                    fieldName = field.getName();
                }
                if (columnName.equals(fieldName)) {
                    columnField = field;
                    break;
                }
            }
            if (columnField != null) {
                mCacheMap.put(columnName, columnField);
            }
        }
    }

    /**
     * （key--字段名，value--值）
     *
     * @param entity 字节码类型
     * @return 映射表
     */
    private Map<String, String> getValues(T entity) {
        HashMap<String, String> map = new HashMap<>();
        for (Field field : mCacheMap.values()) {
            field.setAccessible(true);
            // 获取成员变量的值
            try {
                Object object = field.get(entity);
                if (object == null) {
                    continue;
                }
                String value = object.toString();
                // 获取列名
                String key;
                if (field.getAnnotation(DbField.class) != null) {
                    key = field.getAnnotation(DbField.class).value();
                } else {
                    key = field.getName();
                }
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    map.put(key, value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    private ContentValues getContentValues(Map<String, String> map) {
        ContentValues contentValues = new ContentValues();
        Set<String> keys = map.keySet();
        for (String key : keys) {
            String value = map.get(key);
            if (value != null) {
                contentValues.put(key, value);
            }
        }
        return contentValues;
    }

    /**
     * 根据字段和值转换成SQL条件语句
     */
    private class Condition {
        // 条件语句，类似"name=? and password=?"
        private String whereClasue;
        // 与上面的参数对应的值，类似new String[]{"jett"}
        private String[] whereArgs;

        Condition(Map<String, String> whereClasue) {
            // whereArgs里面的值存入list
            ArrayList list = new ArrayList();
            StringBuilder sb = new StringBuilder();
            sb.append(" 1=1 ");
            // 取所有的字段名
            Set keys = whereClasue.keySet();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String value = whereClasue.get(key);
                if (value != null) {
                    sb.append(" and " + key + "=?");
                    list.add(value);
                }
            }
            this.whereClasue = sb.toString();
            this.whereArgs = (String[]) list.toArray(new String[list.size()]);

        }
    }

    /**
     * 游标转实体类集合
     * @param cursor 游标
     * @param obj 用来表示实体类的结构
     * @return 实体对象集合
     */
    private List<T> getResult(Cursor cursor, T obj) {
        ArrayList list = new ArrayList();
        Object item;
        while (cursor.moveToNext()) {
            try {
                item = obj.getClass().newInstance();//new User();
                Iterator iterator = mCacheMap.entrySet().iterator();//字段-成员变量
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    //取列名
                    String columnName = (String) entry.getKey();
                    //然后以列名拿到列名在游标中的位置
                    Integer columnIndex = cursor.getColumnIndex(columnName);

                    Field field = (Field) entry.getValue();
                    Class type = field.getType();

                    if (columnIndex != -1) {
                        if (type == String.class) {
                            field.set(item, cursor.getString(columnIndex));
                        } else if (type == Double.class) {
                            field.set(item, cursor.getDouble(columnIndex));
                        } else if (type == Integer.class) {
                            field.set(item, cursor.getInt(columnIndex));
                        } else if (type == Long.class) {
                            field.set(item, cursor.getLong(columnIndex));
                        } else if (type == byte[].class) {
                            field.set(item, cursor.getBlob(columnIndex));
                        } else {
                            continue;
                        }
                    }
                }
                list.add(item);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        return list;
    }
}
