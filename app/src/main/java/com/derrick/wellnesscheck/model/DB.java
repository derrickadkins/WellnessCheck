package com.derrick.wellnesscheck.model;

import static com.derrick.wellnesscheck.WellnessCheck.db;

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
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.derrick.wellnesscheck.model.data.*;

import java.util.List;

@Database(entities = {Contact.class, Settings.class, Entry.class}, version = 1, exportSchema = false)
public abstract class DB extends RoomDatabase {
    private final static String TAG = "DB";
    public abstract ContactDao contactDao();
    public abstract SettingsDao settingsDao();
    public abstract LogDao logDao();

    public Settings settings;
    public Contacts contacts;
    public Log log;

    public interface DbListener {
        void onDbReady(DB db);
    }

    synchronized static void InitDbSync(Context context, DbListener dbListener, boolean settings, boolean contacts, boolean log){
        if(db == null)
            db = Room.databaseBuilder(context,
                    DB.class, "database-name")
                    .fallbackToDestructiveMigration()
                    .build();

        if(settings && db.settings == null) db.settings = Settings.Init();
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
        new Thread(() -> InitDbSync(context, dbListener, true, true, true)).start();
    }

    public static void InitDB(Context context, boolean settings, boolean contacts, boolean log){
        DbListener dbListener = null;
        if(context instanceof DbListener)
            dbListener = (DbListener) context;
        InitDB(context, dbListener, settings, contacts, log);
    }

    public static void InitDB(Context context, DbListener dbListener, boolean settings, boolean contacts, boolean log){
        new Thread(() -> InitDbSync(context, dbListener, settings, contacts, log)).start();
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

    @Dao
    public interface SettingsDao {
        @Query("SELECT * FROM settings LIMIT 1")
        Settings getSettings();

        @Update
        void update(Settings settings);

        @Insert
        void insert(Settings settings);
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `Fruit` (`id` INTEGER, "
                    + "`name` TEXT, PRIMARY KEY(`id`))");
        }
    };

//    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
//        @Override
//        public void migrate(SupportSQLiteDatabase database) {
//            database.execSQL("ALTER TABLE Book "
//                    + " ADD COLUMN pub_year INTEGER");
//        }
//    };
}