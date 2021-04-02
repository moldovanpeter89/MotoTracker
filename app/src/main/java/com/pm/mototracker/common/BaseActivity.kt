package com.pm.mototracker.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KClass

abstract class BaseActivity<V : ViewBinding, M : ViewModel> : AppCompatActivity() {
    protected lateinit var binding: V
    protected lateinit var viewModel: M

    abstract fun viewModelClass(): KClass<M>

    abstract fun provideBinding(): V

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = provideBinding()
        setContentView(binding.root)
    }
}