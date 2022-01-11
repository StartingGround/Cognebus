package com.startingground.cognebus.createorrenamefolderorfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentCreateFolderOrFileSelectionBinding
import com.startingground.cognebus.directories.DirectoriesFragment

class CreateFolderOrFileSelectionFragment : Fragment() {

    private lateinit var binding: FragmentCreateFolderOrFileSelectionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_create_folder_or_file_selection,
            container,
            false)
        return binding.root
    }


    var folderId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let{
            folderId = it.getLong("folderId")
            if(it.getBoolean("folderIdIsNull")){
                folderId = null
            }
        }

        binding.createFolderOrFileSelectionFragment = this
    }


    fun onFolderButton(){
        val action = CreateFolderOrFileSelectionFragmentDirections
            .actionCreateFolderOrFileSelectionFragmentToCreateOrRenameFolderOrFileFragment(
                folderId ?: 0L,
                folderId == null,
                DirectoriesFragment.TYPE_FOLDER
            )
        findNavController().navigate(action)
    }

    fun onFileButton(){
        val action = CreateFolderOrFileSelectionFragmentDirections
            .actionCreateFolderOrFileSelectionFragmentToCreateOrRenameFolderOrFileFragment(
                folderId ?: 0L,
                folderId == null,
                DirectoriesFragment.TYPE_FILE
            )
        findNavController().navigate(action)
    }
}