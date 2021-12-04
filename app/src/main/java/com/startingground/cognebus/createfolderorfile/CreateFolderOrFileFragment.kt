package com.startingground.cognebus.createfolderorfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.startingground.cognebus.DataViewModel
import com.startingground.cognebus.DataViewModelFactory
import com.startingground.cognebus.R
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.databinding.FragmentCreateFolderOrFileBinding
import com.startingground.cognebus.directories.DirectoriesFragment
class CreateFolderOrFileFragment : Fragment() {

    private lateinit var createViewModel: CreateViewModel
    private lateinit var dataViewModel: DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var folderId: Long? = null
        var inputType = DirectoriesFragment.TYPE_FOLDER

        arguments?.let {
            folderId = it.getLong("folderId")
            if(it.getBoolean("folderIdIsNull")){
                folderId = null
            }

            inputType = it.getInt("inputType")
        }

        val application = requireNotNull(this.activity).application
        val database = CognebusDatabase.getInstance(application)

        val dataViewModelFactory = DataViewModelFactory(application)
        dataViewModel = ViewModelProvider(this.requireActivity(), dataViewModelFactory)
            .get(DataViewModel::class.java)

        val createViewModelFactory = CreateViewModelFactory(
            database,
            folderId,
            inputType,
            dataViewModel,
            application
        )
        createViewModel = ViewModelProvider(this, createViewModelFactory).get(CreateViewModel::class.java)
    }


    private lateinit var binding: FragmentCreateFolderOrFileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_folder_or_file, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createViewModel = createViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        when(createViewModel.inputType) {
            DirectoriesFragment.TYPE_FOLDER -> {
                binding.createFolderOrFileButton.text = getString(R.string.create_folder_or_file_fragment_create_folder_button)
                binding.createFolderOrFileEditText.setHint(R.string.create_folder_or_file_fragment_folder_edit_text_hint)
            }
            DirectoriesFragment.TYPE_FILE -> {
                binding.createFolderOrFileButton.text = getString(R.string.create_folder_or_file_fragment_create_file_button)
                binding.createFolderOrFileEditText.setHint(R.string.create_folder_or_file_fragment_file_edit_text_hint)
            }
        }

        binding.createFolderOrFileEditText.editText?.doOnTextChanged { text, _, _, _ ->
            createViewModel.addFileOrFolderText(text.toString())
            createViewModel.removeErrorText()
        }

        createViewModel.fileOrFolderErrorText.observe(viewLifecycleOwner){
            binding.createFolderOrFileEditText.error = it
        }

        createViewModel.goBackToDirectoriesTrigger.observe(viewLifecycleOwner){
            if(!it) return@observe
            goBackToDirectories()
        }
    }


    private fun goBackToDirectories(){
        findNavController().popBackStack()
        findNavController().popBackStack()
    }
}