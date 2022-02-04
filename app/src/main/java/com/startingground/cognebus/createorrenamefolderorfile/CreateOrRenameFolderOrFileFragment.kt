package com.startingground.cognebus.createorrenamefolderorfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentCreateOrRenameFolderOrFileBinding
import com.startingground.cognebus.directories.DirectoriesFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreateOrRenameFolderOrFileFragment : Fragment() {

    @Inject lateinit var createOrRenameViewModelAssistedFactory: CreateOrRenameViewModelAssistedFactory
    private lateinit var createOrRenameViewModel: CreateOrRenameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var folderId: Long? = null
        var inputType = DirectoriesFragment.TYPE_FOLDER
        var existingItemId: Long? = null

        arguments?.let {
            folderId = it.getLong("folderId")
            if(it.getBoolean("folderIdIsNull")){
                folderId = null
            }

            inputType = it.getInt("inputType")

            existingItemId = it.getLong("existingItemId")
            if(existingItemId == 0L) existingItemId = null
        }

        val createOrRenameViewModelFactory = CreateOrRenameViewModelFactory(
            createOrRenameViewModelAssistedFactory,
            folderId,
            inputType,
            existingItemId
        )
        createOrRenameViewModel = ViewModelProvider(this, createOrRenameViewModelFactory)[CreateOrRenameViewModel::class.java]
    }


    private lateinit var binding: FragmentCreateOrRenameFolderOrFileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_or_rename_folder_or_file, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createOrRenameViewModel = createOrRenameViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        when(createOrRenameViewModel.inputType) {
            DirectoriesFragment.TYPE_FOLDER -> {

                binding.saveFolderOrFileButton.text =
                    if(createOrRenameViewModel.existingFolderOrFileId == null){
                        getString(R.string.create_or_rename_folder_or_file_fragment_create_folder_button)
                    } else{
                        getString(R.string.create_or_rename_folder_or_file_fragment_rename_folder_button)
                    }

                binding.folderOrFileEditText.setHint(R.string.create_or_rename_folder_or_file_fragment_folder_edit_text_hint)

            }

            DirectoriesFragment.TYPE_FILE -> {

                binding.saveFolderOrFileButton.text =
                    if(createOrRenameViewModel.existingFolderOrFileId == null){
                        getString(R.string.create_or_rename_folder_or_file_fragment_create_file_button)
                    } else{
                        getString(R.string.create_or_rename_folder_or_file_fragment_rename_file_button)
                    }

                binding.folderOrFileEditText.setHint(R.string.create_or_rename_folder_or_file_fragment_file_edit_text_hint)

            }

        }

        binding.folderOrFileEditText.editText?.doOnTextChanged { text, _, _, _ ->
            createOrRenameViewModel.addFileOrFolderText(text.toString())
            createOrRenameViewModel.removeErrorText()
        }

        createOrRenameViewModel.fileOrFolderErrorText.observe(viewLifecycleOwner){
            binding.folderOrFileEditText.error = it
        }

        createOrRenameViewModel.goBackToDirectoriesTrigger.observe(viewLifecycleOwner){
            if(!it) return@observe
            goBackToDirectories()
        }
    }


    private fun goBackToDirectories(){
        findNavController().popBackStack()
        if(createOrRenameViewModel.existingFolderOrFileId != null) return
        findNavController().popBackStack()
    }
}