package com.thermoapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ThermoViewModelFactory(
    private val repo: ThermoRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThermoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThermoViewModel(repo) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: $modelClass")
    }
}