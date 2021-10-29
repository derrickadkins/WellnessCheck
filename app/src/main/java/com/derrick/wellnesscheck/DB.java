package com.derrick.wellnesscheck;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.Update;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

@Database(entities = {Contact.class, AppSettings.class, LogEntry.class}, version = 1, exportSchema = false)
public abstract class DB extends RoomDatabase {
    public abstract ContactDao contactDao();
    public abstract SettingsDao settingsDao();
    public abstract LogDao logDao();

    @Dao
    public interface LogDao {
        @Query("SELECT * FROM logEntry ORDER BY time DESC")
        List<LogEntry> getAll();

        @Insert
        void insert(LogEntry... logEntries);

        @Query("DELETE FROM LogEntry")
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
        @Query("SELECT * FROM appsettings LIMIT 1")
        AppSettings getSettings();

        @Update
        void update(AppSettings appSettings);

        @Insert
        void insert(AppSettings appSettings);
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