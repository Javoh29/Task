package com.range.task;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLocation(LocationModel model);

    @Query("select * from location_table")
    LiveData<List<LocationModel>> getAllLocation();

    @Query("DELETE FROM location_table")
    void deleteAllLocation();
}
