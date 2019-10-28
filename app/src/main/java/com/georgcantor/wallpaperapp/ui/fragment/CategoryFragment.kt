package com.georgcantor.wallpaperapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.georgcantor.wallpaperapp.R
import com.georgcantor.wallpaperapp.ui.adapter.CategoryAdapter
import com.georgcantor.wallpaperapp.util.*
import com.georgcantor.wallpaperapp.viewmodel.CategoryViewModel
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_common.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

class CategoryFragment : Fragment() {

    companion object {
        fun newInstance(): CategoryFragment {
            val fragment = CategoryFragment()
            val args = Bundle()
            fragment.arguments = args

            return fragment
        }
    }

    private lateinit var viewModel: CategoryViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getViewModel { parametersOf() }
        preferenceManager = PreferenceManager(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_common, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!requireActivity().isNetworkAvailable()) {
            noInternetImageView.visibility = View.VISIBLE
        }
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = GridLayoutManager(activity, requireContext().getScreenSize())

        categoryAdapter = CategoryAdapter(requireContext())
        recyclerView.adapter = categoryAdapter

        refreshLayout.setOnRefreshListener {
            loadData()
            refreshLayout.isRefreshing = false
        }

        loadData()

        val hideScrollListener = object : HideNavScrollListener(requireActivity().navigation) {}
        recyclerView.addOnScrollListener(hideScrollListener)
    }

    private fun loadData() {
        if (preferenceManager.getCategories("cat").isNullOrEmpty()) {
            val disposable = viewModel.getCategories(preferenceManager)
                .doOnSubscribe {
                    animationView?.showAnimation()
                }
                .doFinally {
                    animationView?.hideAnimation()
                }
                .subscribe(categoryAdapter::setCategoryList) {
                    requireActivity().longToast(it.message.toString())
                }

            DisposableManager.add(disposable)
        } else {
            val disposable = viewModel.getSavedCategories(preferenceManager)
                .doOnSubscribe {
                    animationView?.showAnimation()
                }
                .doFinally {
                    animationView?.hideAnimation()
                }
                .subscribe(categoryAdapter::setCategoryList) {
                    requireActivity().longToast(it.message.toString())
                }

            DisposableManager.add(disposable)
        }
    }

    override fun onDestroy() {
        DisposableManager.dispose()
        super.onDestroy()
    }

}
