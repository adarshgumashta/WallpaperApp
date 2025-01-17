package com.georgcantor.wallpaperapp.ui.activity.detail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgcantor.wallpaperapp.model.local.Favorite
import com.georgcantor.wallpaperapp.model.remote.response.CommonPic
import com.georgcantor.wallpaperapp.repository.Repository
import com.google.gson.Gson
import kotlinx.coroutines.launch

class DetailViewModel(private val repo: Repository) : ViewModel() {

    val isFavorite = MutableLiveData<Boolean>()

    fun isFavorite(url: String?) {
        viewModelScope.launch { isFavorite.postValue(repo.isFavorite(url)) }
    }

    fun addOrRemoveFromFavorites(pic: CommonPic?) {
        viewModelScope.launch {
            isFavorite.value?.also { favorite ->
                isFavorite.postValue(!favorite)
                if (favorite) repo.removeFromFavorites(pic?.url)
                else repo.addToFavorites(Favorite(pic?.url ?: "", Gson().toJson(pic)))
            }
        }
    }
}