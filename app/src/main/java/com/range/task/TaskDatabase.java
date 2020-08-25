package com.range.task;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {LocationModel.class},
        version = 1
)
public abstract class TaskDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
}
