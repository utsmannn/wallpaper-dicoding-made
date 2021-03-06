/*
 * Created by Muhammad Utsman on 6/2/21 10:01 PM
 * Copyright (c) 2021
 */

package com.utsman.domain.usecase

import androidx.lifecycle.MutableLiveData
import androidx.paging.*
import com.utsman.data.local.database.FavoriteDataBase
import com.utsman.data.remote.Services
import com.utsman.data.remote.interactor.ResultState
import com.utsman.data.remote.interactor.fetchApi
import com.utsman.data.remote.source.WallpaperPagedSource
import com.utsman.domain.entity.Wallpaper
import com.utsman.domain.utils.DataMapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

class WallpaperInteractor(
    private val services: Services,
    private val favoriteDataBase: FavoriteDataBase,
) : WallpapersUseCase {
    override val wallpaperPaged = MutableLiveData<PagingData<Wallpaper>>()
    override val wallpaperDetail = MutableLiveData<ResultState<Wallpaper>>()

    override suspend fun getWallpaperPaged() {
        Pager(PagingConfig(pageSize = 10)) {
            WallpaperPagedSource(services)
        }.flow
            .cachedIn(GlobalScope)
            .collect { p ->
                wallpaperPaged.postValue(p.map { d -> DataMapper.mapResponseToWallpaper(d) })
            }
    }

    override suspend fun getDetailWallpaper(id: String) {
        fetchApi {
            services.getDetail(id)
        }.map {
            DataMapper.mapResultState(it) { response ->
                DataMapper.mapDetailToWallpaper(response)
            }
        }.collect {
            wallpaperDetail.postValue(it)
        }
    }

    override val hasFavorite = MutableLiveData<Boolean>()
    override suspend fun addFavorite(wallpaper: Wallpaper) {
        val entity = DataMapper.mapWallpaperToEntity(wallpaper)
        favoriteDataBase.favoriteDao().insertFavorite(entity)
    }

    override suspend fun removeFavorite(wallpaper: Wallpaper) {
        favoriteDataBase.favoriteDao().deleteFavorite(wallpaper.id)
    }

    override suspend fun checkFavorite(wallpaperId: String) {
        favoriteDataBase.favoriteDao()
            .getById(wallpaperId)
            .collect { entity ->
                val added = entity != null
                hasFavorite.postValue(added)
            }
    }
}