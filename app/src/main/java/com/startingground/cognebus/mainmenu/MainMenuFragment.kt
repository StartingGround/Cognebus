package com.startingground.cognebus.mainmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentMainMenuBinding
import com.startingground.cognebus.practice.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private lateinit var binding: FragmentMainMenuBinding
    private val sharedPracticeViewModel: PracticeViewModel by activityViewModels()
    private val mainMenuViewModel: MainMenuViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_menu, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mainMenuFragment = this
        binding.mainMenuViewModel = mainMenuViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.repetition.text = getString(R.string.repetition_button, 0)

        mainMenuViewModel.numberOfFlashcardsForRepetition.observe(viewLifecycleOwner){
            binding.repetition.text = getString(R.string.repetition_button, it)
        }
    }


    override fun onStart() {
        super.onStart()
        mainMenuViewModel.reloadFlashcardsForRepetition()
    }


    fun onFilesButtonClicked(){
        findNavController().navigate(R.id.action_mainMenuFragment_to_directoriesFragment)
    }


    fun onRepetitionButtonClicked(){
        val flashcardsForRepetition = mainMenuViewModel.getFlashcardsForRepetition().toMutableList()
        val filesForRepetition = mainMenuViewModel.getFilesForRepetition().toMutableList()

        sharedPracticeViewModel.setFlashcards(flashcardsForRepetition, filesForRepetition, true)
        findNavController().navigate(R.id.action_mainMenuFragment_to_practicePagerFragment2)
    }


    fun onSettingsButtonClicked(){
        findNavController().navigate(R.id.action_mainMenuFragment_to_settingsFragment)
    }
}