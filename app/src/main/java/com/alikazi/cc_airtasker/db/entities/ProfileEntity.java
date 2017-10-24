package com.alikazi.cc_airtasker.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.alikazi.cc_airtasker.db.type_converters.MiniUrlConverter;

/**
 * Created by alikazi on 21/10/17.
 */

@Entity
public class ProfileEntity {
    @NonNull
    public @PrimaryKey int id;
    public @TypeConverters(MiniUrlConverter.class) String avatar_mini_url;
    public String first_name;
    public int rating;
}
