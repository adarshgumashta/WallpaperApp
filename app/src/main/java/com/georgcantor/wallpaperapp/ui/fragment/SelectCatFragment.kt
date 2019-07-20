package com.georgcantor.wallpaperapp.ui.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.georgcantor.wallpaperapp.MyApplication
import com.georgcantor.wallpaperapp.R
import com.georgcantor.wallpaperapp.model.Hit
import com.georgcantor.wallpaperapp.model.Pic
import com.georgcantor.wallpaperapp.network.ApiService
import com.georgcantor.wallpaperapp.ui.adapter.WallpAdapter
import com.georgcantor.wallpaperapp.ui.util.EndlessRecyclerViewScrollListener
import com.georgcantor.wallpaperapp.ui.util.UtilityMethods
import kotlinx.android.synthetic.main.fragment_select_cat.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import javax.inject.Inject

class SelectCatFragment : Fragment() {

    companion object {
        const val EXTRA_CAT = "category"
    }

    @Inject
    lateinit var retrofit: Retrofit

    lateinit var adapter: WallpAdapter
    private var type: String? = null
    private var picResult: Pic? = Pic()
    private var columnNo: Int = 0

    override fun onAttach(context: Context?) {
        (MyApplication.instance as MyApplication)
                .getApiComponent()
                .inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_cat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectCatRecyclerView.setHasFixedSize(true)
        type = arguments?.getString(EXTRA_CAT)

        if (!UtilityMethods.isNetworkAvailable) {
            Toast.makeText(requireContext(), getString(R.string.check_internet), Toast.LENGTH_SHORT).show()
        }
        checkScreenSize()
        val gridLayoutManager = StaggeredGridLayoutManager(columnNo, StaggeredGridLayoutManager.VERTICAL)
        selectCatRecyclerView.layoutManager = gridLayoutManager

        val listener = object : EndlessRecyclerViewScrollListener(gridLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                loadData(type as String, page)
            }
        }
        selectCatRecyclerView.addOnScrollListener(listener)
        adapter = WallpAdapter(requireContext())
        selectCatRecyclerView.adapter = adapter

        loadData(type as String, 1)
    }

    private fun loadData(type: String, index: Int) {
        selectCatProgress?.let { it.visibility = View.VISIBLE }

        val client = retrofit.create(ApiService::class.java)
        client?.let {
            val call = it.getPictures(type, index)
            call.enqueue(object : Callback<Pic> {
                override fun onResponse(call: Call<Pic>, response: Response<Pic>) {
                    selectCatProgress?.let { it.visibility = View.GONE }
                    try {
                        if (!response.isSuccessful) {
                            Log.d(resources.getString(R.string.No_Success),
                                    response.errorBody()?.string())
                        } else {
                            picResult = response.body()
                            if (picResult != null) {
                                adapter.setPicList(picResult?.hits as MutableList<Hit>)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<Pic>, t: Throwable) {
                    selectCatProgress?.let { it.visibility = View.GONE }
                    Toast.makeText(requireContext(), resources.getString(R.string.wrong_message),
                            Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun checkScreenSize() {
        val screenSize = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        columnNo = when (screenSize) {
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> 4
            Configuration.SCREENLAYOUT_SIZE_UNDEFINED -> 3
            Configuration.SCREENLAYOUT_SIZE_LARGE -> 3
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> 2
            Configuration.SCREENLAYOUT_SIZE_SMALL -> 2
            else -> 2
        }
    }
}