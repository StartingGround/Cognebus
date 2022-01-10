package com.startingground.cognebus.file

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.sharedviewmodels.DataViewModelFactory
import com.startingground.cognebus.R
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.databinding.FragmentFileBinding
import com.startingground.cognebus.practice.PracticeViewModel
import com.startingground.cognebus.practice.PracticeViewModelFactory

class FileFragment : Fragment() {

    private lateinit var fileViewModel: FileViewModel
    private lateinit var sharedPracticeViewModel: PracticeViewModel
    private var title: String? = null
    private var fileId: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = requireNotNull(this.activity).application
        val database = CognebusDatabase.getInstance(application)

        arguments?.let {
            fileId = it.getLong("fileId")
            title = it.getString("title")
        }

        val dataViewModelFactory = DataViewModelFactory(application)
        val dataViewModel = ViewModelProvider(this.requireActivity(), dataViewModelFactory).get(DataViewModel::class.java)

        val fileViewModelFactory = FileViewModelFactory(database, fileId, dataViewModel)
        fileViewModel = ViewModelProvider(this, fileViewModelFactory).get(FileViewModel::class.java)

        val practiceViewModelFactory = PracticeViewModelFactory(application, database, dataViewModel)
        sharedPracticeViewModel = ViewModelProvider(this.requireActivity(), practiceViewModelFactory).get(PracticeViewModel::class.java)
    }


    private lateinit var binding: FragmentFileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_file, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileFragment = this
        binding.fileViewModel = fileViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.topAppBar.title = title

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        fileViewModel.cycleIncrementError.observe(viewLifecycleOwner){
            binding.cycleIncrementEditText.error = it
        }

        fileViewModel.maxDaysPerCycleError.observe(viewLifecycleOwner){
            binding.maxDaysPerCycleEditText.error = it
        }

        binding.practiceOrderDropdownTextView.setOnItemClickListener { _, _, position, _ ->
            fileViewModel.onSortingSelected(position.toLong())
            binding.practiceOrderDropdownTextView.clearFocus()
        }

        binding.maxDaysPerCycleEditText.editText?.doOnTextChanged { text, _, _, _ ->
            fileViewModel.onMaxDaysPerCycleChanged(text.toString())
        }

        binding.cycleIncrementEditText.editText?.doOnTextChanged { text, _, _, _ ->
            fileViewModel.onCycleIncrementChanged(text.toString())
        }

        fileViewModel.sortingList.observe(viewLifecycleOwner, sortingListObserver)

        binding.practiceButton.text = getString(R.string.file_fragment_practice_button, 0, 0)

        fileViewModel.numberOfFlashcardsForPractice.observe(viewLifecycleOwner){
            val (numberOfFlashcardsForPractice, totalNumberOfFlashcards) = it
            binding.practiceButton.text = getString(R.string.file_fragment_practice_button, numberOfFlashcardsForPractice, totalNumberOfFlashcards)
        }
    }


    private val sortingListObserver = Observer<Array<String>>{
        val adapter = ArrayAdapter(requireContext(), R.layout.fragment_file_practice_order_dropdown_item, it)
        binding.practiceOrderDropdownTextView.setAdapter(adapter)
        if(it.isNotEmpty()) {
            fileViewModel.file.observe(viewLifecycleOwner, filePracticeOrderSetupListener)
        }
    }


    private val filePracticeOrderSetupListener = Observer<FileDB> {
        val adapter = binding.practiceOrderDropdownTextView.adapter
        val text = adapter.getItem(it.sortingId.toInt() - 1).toString()
        binding.practiceOrderDropdownTextView.setText(text, false)
    }


    fun onAddFlashcard(){
        val action = FileFragmentDirections.actionFileFragmentToFlashcardPagerFragment(fileId, 0L)
        findNavController().navigate(action)
    }

    fun onPractice() {
        val flashcardsForPractice = fileViewModel.getFlashcardsForPractice()
        val files = fileViewModel.getFilesForPractice()

        sharedPracticeViewModel.setFlashcards(flashcardsForPractice, files, false)
        findNavController().navigate(R.id.action_fileFragment_to_practicePagerFragment)
    }

    fun onViewFlashcards(){
        val action = FileFragmentDirections.actionFileFragmentToFlashcardsListFragment(fileId, fileViewModel.file.value?.name ?: "", fileViewModel.file.value?.enableHtml ?: false)
        findNavController().navigate(action)
    }
}