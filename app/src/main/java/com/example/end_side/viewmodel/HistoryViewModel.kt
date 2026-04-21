package com.example.end_side.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.end_side.data.AppDatabase
import com.example.end_side.data.entity.StudySession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    val sessions = MutableLiveData<List<StudySession>>(emptyList())

    fun loadSessions(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(context).studySessionDao()
                val all = dao.getAll()
                sessions.postValue(all)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
