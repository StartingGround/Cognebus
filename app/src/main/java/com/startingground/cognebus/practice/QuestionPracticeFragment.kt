package com.startingground.cognebus.practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.sharedviewmodels.DataViewModelFactory
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentQuestionPracticeBinding

class QuestionPracticeFragment : Fragment() {

    private var binding: FragmentQuestionPracticeBinding? = null
    private var sharedPracticeViewModel: PracticeViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = requireNotNull(this.activity).application

        val dataViewModelFactory = DataViewModelFactory(application)
        val dataViewModel = ViewModelProvider(this.requireActivity(), dataViewModelFactory).get(DataViewModel::class.java)

        val practiceViewModelFactory = PracticeViewModelFactory(application, dataViewModel)
        sharedPracticeViewModel = ViewModelProvider(this.requireActivity(), practiceViewModelFactory).get(PracticeViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_question_practice, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.sharedPracticeViewModel = sharedPracticeViewModel
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.questionMathView?.settings?.allowFileAccess = true

        sharedPracticeViewModel?.currentFlashcard?.observe(viewLifecycleOwner){
            if(it == null) {
                findNavController().popBackStack()
            }
        }

        binding?.topAppBar?.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        val viewPager: ViewPager2? =  activity?.findViewById(R.id.practice_pager)

        binding?.topAppBar?.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.move_to_answer ->{
                    viewPager?.currentItem = 1
                    true
                }
                else -> false
            }
        }
    }
}