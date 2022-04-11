package org.edx.mobile.view.dialog

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import org.edx.mobile.R
import org.edx.mobile.databinding.DialogFullscreenLoaderBinding
import org.edx.mobile.util.NonNullObserver
import org.edx.mobile.viewModel.InAppPurchasesViewModel

class FullscreenLoaderDialogFragment : DialogFragment() {

    private lateinit var binding: DialogFullscreenLoaderBinding

    private val iapViewModel: InAppPurchasesViewModel
            by viewModels(ownerProducer = { requireActivity() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.AppTheme_NoActionBar
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogFullscreenLoaderBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, args: Bundle?) {
        super.onViewCreated(view, args)
        intiViews()
        initObservers()
        if (iapViewModel.isExecuted().not()) {
            iapViewModel.executeOrder()
        }
    }

    private fun intiViews() {
        binding.materialTextView.setText(getMessage(), TextView.BufferType.SPANNABLE)
    }

    private fun initObservers() {
        iapViewModel.executeOrderResponse.observe(viewLifecycleOwner, NonNullObserver {
            iapViewModel.refreshCourseData()
            iapViewModel.reset()
        })
    }

    private fun getMessage(): SpannableStringBuilder {
        val unlocking = getString(R.string.fullscreen_loader_unlocking)
        val fullAccess = getString(R.string.fullscreen_loader_full_access)
        val toYourCourse = getString(R.string.fullscreen_loader_to_your_course)

        val spannable =
            SpannableStringBuilder(String.format("%s\n%s\n%s", unlocking, fullAccess, toYourCourse))

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.accentAColor)),
            unlocking.length,
            unlocking.length + fullAccess.length + 1,
            Spannable.SPAN_EXCLUSIVE_INCLUSIVE
        )
        return spannable
    }

    companion object {
        const val TAG = "FULLSCREEN_LOADER"
        const val DELAY: Long = 3_000

        @JvmStatic
        fun newInstance(): FullscreenLoaderDialogFragment {
            return FullscreenLoaderDialogFragment()
        }
    }
}
