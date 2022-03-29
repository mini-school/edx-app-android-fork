package org.edx.mobile.view.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.edx.mobile.R
import org.edx.mobile.core.IEdxEnvironment
import org.edx.mobile.databinding.DialogUpgradeFeaturesBinding
import org.edx.mobile.extenstion.setVisibility
import org.edx.mobile.http.HttpStatus
import org.edx.mobile.http.notifications.SnackbarErrorNotification
import org.edx.mobile.inapppurchases.BillingProcessor
import org.edx.mobile.inapppurchases.CourseUpgradeListener
import org.edx.mobile.inapppurchases.ProductManager
import org.edx.mobile.util.AppConstants
import org.edx.mobile.util.InAppPurchasesException
import org.edx.mobile.util.NonNullObserver
import org.edx.mobile.util.ResourceUtil
import org.edx.mobile.viewModel.InAppPurchasesViewModel
import javax.inject.Inject

@AndroidEntryPoint
class CourseModalDialogFragment : DialogFragment() {

    private lateinit var binding: DialogUpgradeFeaturesBinding
    private var screenName: String = ""
    private var courseId: String = ""
    private var price: String = ""
    private var isSelfPaced: Boolean = false
    private lateinit var courseUpgradeListener: CourseUpgradeListener

    private var billingProcessor: BillingProcessor? = null
    private val iapViewModel: InAppPurchasesViewModel by viewModels()

    @Inject
    lateinit var environment: IEdxEnvironment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.AppTheme_NoActionBar
        )
        courseUpgradeListener = parentFragment as CourseUpgradeListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogUpgradeFeaturesBinding.inflate(inflater)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogSlideUpAndDownAnimation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        if (environment.config.isIAPEnabled) {
            initBillingProcessor()
        }
    }

    private fun initViews() {
        arguments?.let { bundle ->
            screenName = bundle.getString(KEY_SCREEN_NAME, "")
            courseId = bundle.getString(KEY_COURSE_ID, "")
            price = bundle.getString(KEY_COURSE_PRICE, "")
            isSelfPaced = bundle.getBoolean(KEY_IS_SELF_PACED)
            environment.analyticsRegistry.trackValuePropLearnMoreTapped(
                courseId, null, screenName
            )
            environment.analyticsRegistry.trackValuePropModalView(
                courseId, null, screenName
            )
        }

        binding.dialogTitle.text = ResourceUtil.getFormattedString(
            resources,
            R.string.course_modal_heading,
            KEY_COURSE_NAME,
            arguments?.getString(KEY_COURSE_NAME)
        )
        binding.layoutUpgradeBtn.root.setVisibility(environment.config.isIAPEnabled)
        binding.dialogDismiss.setOnClickListener {
            dialog?.dismiss()
        }
    }

    private fun initBillingProcessor() {
        initObserver()

        binding.layoutUpgradeBtn.btnUpgrade.setOnClickListener {
            ProductManager.getProductByCourseId(courseId)?.let {
                iapViewModel.addProductToBasket(it)
            } ?: showUpgradeErrorDialog()
            environment.analyticsRegistry.trackUpgradeNowClicked(
                courseId,
                price,
                null,
                isSelfPaced
            )
        }
        billingProcessor =
            BillingProcessor(requireContext(), object : BillingProcessor.BillingFlowListeners {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    super.onBillingSetupFinished(billingResult)
                    // Shimmer container taking sometime to get ready and perform the animation, so
                    // by adding the some delay fixed that issue for lower-end devices, and for the
                    // proper animation.
                    binding.layoutUpgradeBtn.shimmerViewContainer.postDelayed({
                        initializeProductPrice()
                    }, 1500)
                    binding.layoutUpgradeBtn.btnUpgrade.isEnabled = false
                }

                override fun onPurchaseCancel() {
                    iapViewModel.endLoading()
                    showUpgradeErrorDialog(
                        errorResId = R.string.error_payment_not_processed
                    )
                }

                override fun onPurchaseComplete(purchase: Purchase) {
                    onProductPurchased(purchase.purchaseToken)
                }
            })
    }

    private fun initializeProductPrice() {
        ProductManager.getProductByCourseId(courseId)?.let {
            billingProcessor?.querySyncDetails(
                productId = it
            ) { _, skuDetails ->
                val skuDetail = skuDetails?.get(0)
                if (skuDetail?.sku == it) {
                    binding.layoutUpgradeBtn.btnUpgrade.text =
                        ResourceUtil.getFormattedString(
                            resources,
                            R.string.label_upgrade_course_button,
                            AppConstants.PRICE,
                            skuDetail.price
                        ).toString()
                    // The app get the sku details instantly, so add some wait to perform
                    // animation at least one cycle.
                    binding.layoutUpgradeBtn.shimmerViewContainer.postDelayed({
                        binding.layoutUpgradeBtn.shimmerViewContainer.hideShimmer()
                        binding.layoutUpgradeBtn.btnUpgrade.isEnabled = true
                    }, 500)
                } else {
                    showUpgradeErrorDialog(errorResId = R.string.error_price_not_fetched)
                }
            }
        } ?: showUpgradeErrorDialog(errorResId = R.string.error_price_not_fetched)
    }

    private fun initObserver() {
        iapViewModel.showLoader.observe(viewLifecycleOwner, NonNullObserver {
            enableUpgradeButton(!it)
        })

        iapViewModel.checkoutResponse.observe(viewLifecycleOwner, NonNullObserver {
            if (it.paymentPageUrl.isNotEmpty())
                purchaseProduct(iapViewModel.getProductId())
        })

        iapViewModel.executeOrderResponse.observe(viewLifecycleOwner, NonNullObserver {
            showPurchaseSuccessSnackbar()
            dismiss()
            courseUpgradeListener.onComplete()
        })

        iapViewModel.errorMessage.observe(viewLifecycleOwner, NonNullObserver { errorMsg ->
            if (errorMsg.throwable is InAppPurchasesException) {
                when (errorMsg.throwable.httpErrorCode) {
                    HttpStatus.UNAUTHORIZED -> {
                        environment.router?.forceLogout(
                            requireContext(),
                            environment.analyticsRegistry,
                            environment.notificationDelegate
                        )
                        return@NonNullObserver
                    }
                    else -> showUpgradeErrorDialog(errorMsg.errorResId)
                }
            } else {
                showUpgradeErrorDialog(errorMsg.errorResId)
            }
            iapViewModel.errorMessageShown()
        })
    }

    private fun enableUpgradeButton(enable: Boolean) {
        binding.layoutUpgradeBtn.btnUpgrade.setVisibility(enable)
        binding.layoutUpgradeBtn.loadingIndicator.setVisibility(!enable)
    }

    private fun purchaseProduct(productId: String) {
        activity?.let { billingProcessor?.purchaseItem(it, productId) }
    }

    private fun onProductPurchased(purchaseToken: String) {
        lifecycleScope.launch {
            executeOrder(purchaseToken)
        }
    }

    private fun executeOrder(purchaseToken: String) {
        iapViewModel.executeOrder(purchaseToken = purchaseToken)
    }

    private fun showUpgradeErrorDialog(
        @StringRes errorResId: Int = R.string.general_error_message
    ) {
        AlertDialogFragment.newInstance(
            getString(R.string.title_upgrade_error),
            getString(errorResId),
            getString(R.string.label_close),
            null,
            getString(R.string.label_get_help)
        ) { _, _ ->
            environment.router?.showFeedbackScreen(
                requireActivity(),
                getString(R.string.email_subject_upgrade_error)
            )
        }.show(childFragmentManager, null)
    }

    private fun showPurchaseSuccessSnackbar() {
        SnackbarErrorNotification(binding.root).showError(R.string.purchase_success_message)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingProcessor?.disconnect()
    }

    companion object {
        const val TAG: String = "CourseModalDialogFragment"
        const val KEY_MODAL_PLATFORM = "platform_name"
        const val KEY_SCREEN_NAME = "screen_name"
        const val KEY_COURSE_ID = "course_id"
        const val KEY_COURSE_NAME = "course_name"
        const val KEY_COURSE_PRICE = "course_price"
        const val KEY_IS_SELF_PACED = "is_Self_Paced"

        @JvmStatic
        fun newInstance(
            platformName: String,
            screenName: String,
            courseId: String,
            courseName: String,
            price: String,
            isSelfPaced: Boolean,
        ): CourseModalDialogFragment {
            val frag = CourseModalDialogFragment()
            val args = Bundle().apply {
                putString(KEY_MODAL_PLATFORM, platformName)
                putString(KEY_SCREEN_NAME, screenName)
                putString(KEY_COURSE_ID, courseId)
                putString(KEY_COURSE_NAME, courseName)
                putString(KEY_COURSE_PRICE, price)
                putBoolean(KEY_IS_SELF_PACED, isSelfPaced)
            }
            frag.arguments = args
            return frag
        }
    }
}
