package com.startingground.cognebus.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)

        val settingsViewModelFactory = SettingViewModelFactory(this.requireActivity().application)
        settingsViewModel = ViewModelProvider(this, settingsViewModelFactory).get(SettingsViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settingsViewModel = settingsViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        settingsViewModel.cycleIncrementError.observe(viewLifecycleOwner){
            binding.cycleIncrementEditText.error = it
        }

        settingsViewModel.maxDaysPerCycleError.observe(viewLifecycleOwner){
            binding.maxDaysPerCycleEditText.error = it
        }

        binding.cycleIncrementEditText.editText?.doOnTextChanged { text, _, _, _ ->
            settingsViewModel.onCycleIncrementChanged(text.toString())
        }
        binding.maxDaysPerCycleEditText.editText?.doOnTextChanged { text, _, _, _ ->
            settingsViewModel.onMaxDaysPerCycleChanged(text.toString())
        }
    }
}