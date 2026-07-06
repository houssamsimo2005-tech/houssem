package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val data: ProjectData
)

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(ProjectData::class.java)

    @TypeConverter
    fun fromProjectData(data: ProjectData): String {
        return adapter.toJson(data) ?: ""
    }

    @TypeConverter
    fun toProjectData(json: String): ProjectData? {
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
