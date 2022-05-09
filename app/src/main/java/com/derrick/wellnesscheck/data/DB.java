package com.derrick.wellnesscheck.data;

import static com.derrick.wellnesscheck.App.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Update;

import java.util.List;

@Database(entities = {Contact.class, Entry.class}, version = 1, exportSchema = false)
public abstract class DB extends RoomDatabase {
    private final static String TAG = "DB";
    public abstract ContactDao contactDao();
    public abstract LogDao logDao();

    public Contacts contacts;
    public Log log;

    public interface DbListener {
        void onDbReady(DB db);
    }

    synchronized static void InitDbSync(Context context, DbListener dbListener, boolean contacts, boolean log){
        if(db == null)
            db = Room.databaseBuilder(context,
                    DB.class, "database-name")
                    .fallbackToDestructiveMigration()
                    .build();

        if(contacts && db.contacts == null) db.contacts = Contacts.Init();
        if(log && db.log == null) db.log = Log.Init();

        if(dbListener != null) new Handler(Looper.getMainLooper()).post(() -> dbListener.onDbReady(db));
    }

    public static void InitDB(Context context){
        DbListener dbListener = null;
        if(context instanceof DbListener)
            dbListener = (DbListener) context;
        InitDB(context, dbListener);
    }

    public static void InitDB(Context context, DbListener dbListener){
        new Thread(() -> InitDbSync(context, dbListener, true, true)).start();
    }

    public static void InitDB(Context context, boolean contacts, boolean log){
        DbListener dbListener = null;
        if(context instanceof DbListener)
            dbListener = (DbListener) context;
        InitDB(context, dbListener, contacts, log);
    }

    public static void InitDB(Context context, DbListener dbListener, boolean contacts, boolean log){
        new Thread(() -> InitDbSync(context, dbListener, contacts, log)).start();
    }

    @Dao
    public interface LogDao {
        @Query("SELECT * FROM entry ORDER BY time DESC")
        List<Entry> getAll();

        @Insert
        void insert(Entry... entries);

        @Query("DELETE FROM entry")
        void nukeTable();
    }

    @Dao
    public interface ContactDao {
        @Query("SELECT * FROM contact")
        List<Contact> getAll();

        @Query("SELECT * FROM contact WHERE id IN (:contactsIds)")
        List<Contact> loadAllByIds(int[] contactsIds);

        @Query("SELECT * FROM contact WHERE name LIKE :name LIMIT 1")
        Contact findByName(String name);

        @Update
        void update(Contact... contacts);

        @Insert
        void insertAll(Contact... contacts);

        @Delete
        void delete(Contact contact);

        @Query("DELETE FROM contact")
        void nukeTable();
    }
}