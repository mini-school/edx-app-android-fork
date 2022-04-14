package org.edx.mobile.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.edx.mobile.R
import org.edx.mobile.exception.ErrorMessage
import org.edx.mobile.http.model.NetworkResponseCallback
import org.edx.mobile.http.model.Result
import org.edx.mobile.model.iap.AddToBasketResponse
import org.edx.mobile.model.iap.CheckoutResponse
import org.edx.mobile.model.iap.ExecuteOrderResponse
import org.edx.mobile.repositorie.InAppPurchasesRepository
import org.edx.mobile.util.InAppPurchasesException
import javax.inject.Inject

@HiltViewModel
class InAppPurchasesViewModel @Inject constructor(
    private val repository: InAppPurchasesRepository
) : ViewModel() {

    private val _showLoader = MutableLiveData(false)
    val showLoader: LiveData<Boolean> = _showLoader

    private val _errorMessage = MutableLiveData<ErrorMessage?>()
    val errorMessage: LiveData<ErrorMessage?> = _errorMessage

    private val _checkoutResponse = MutableLiveData<CheckoutResponse>()
    val checkoutResponse: LiveData<CheckoutResponse> = _checkoutResponse

    private val _executeOrderResponse = MutableLiveData<ExecuteOrderResponse>()
    val executeOrderResponse: LiveData<ExecuteOrderResponse> = _executeOrderResponse

    private val _displayFullscreenLoaderDialog = MutableLiveData(false)
    val displayFullscreenLoaderDialog: LiveData<Boolean> = _displayFullscreenLoaderDialog

    private val _refreshCourseData = MutableLiveData(false)
    val refreshCourseData: LiveData<Boolean> = _refreshCourseData

    private val _processComplete = MutableLiveData(false)
    val processComplete: LiveData<Boolean> = _processComplete

    private var _productId: String = ""
    val productId: String
        get() = _productId

    private var _isVerificationPending = true
    val isVerificationPending: Boolean
        get() = _isVerificationPending

    private var basketId: Long = 0
    private var purchaseToken: String = ""

    fun addProductToBasket(productId: String) {
        _productId = productId
        startLoading()
        repository.addToBasket(
            productId = productId,
            callback = object : NetworkResponseCallback<AddToBasketResponse> {
                override fun onSuccess(result: Result.Success<AddToBasketResponse>) {
                    result.data?.let {
                        proceedCheckout(it.basketId)
                    } ?: endLoading()
                }

                override fun onError(error: Result.Error) {
                    endLoading()
                    setError(ErrorMessage.ADD_TO_BASKET_CODE, error.throwable)
                }
            })
    }

    fun proceedCheckout(basketId: Long) {
        this.basketId = basketId
        repository.proceedCheckout(
            basketId = basketId,
            callback = object : NetworkResponseCallback<CheckoutResponse> {
                override fun onSuccess(result: Result.Success<CheckoutResponse>) {
                    result.data?.let {
                        _checkoutResponse.value = it
                    } ?: endLoading()
                }

                override fun onError(error: Result.Error) {
                    endLoading()
                    setError(ErrorMessage.CHECKOUT_CODE, error.throwable)
                }
            })
    }

    fun executeOrder() {
        repository.executeOrder(
            basketId = basketId,
            productId = productId,
            purchaseToken = purchaseToken,
            callback = object : NetworkResponseCallback<ExecuteOrderResponse> {
                override fun onSuccess(result: Result.Success<ExecuteOrderResponse>) {
                    result.data?.let {
                        _executeOrderResponse.value = it
                        orderExecuted()
                        refreshCourseData()
                    }
                    endLoading()
                }

                override fun onError(error: Result.Error) {
                    endLoading()
                    setError(ErrorMessage.EXECUTE_ORDER_CODE, error.throwable)
                }
            })
    }

    private fun orderExecuted() {
        _productId = ""
        basketId = 0
        _isVerificationPending = false
    }

    fun setError(errorCode: Int, throwable: Throwable) {
        _errorMessage.value = ErrorMessage(errorCode, throwable, getErrorMessage(throwable))
    }

    fun errorMessageShown() {
        _errorMessage.value = null
    }

    private fun getErrorMessage(throwable: Throwable) = if (throwable is InAppPurchasesException) {
        when (throwable.httpErrorCode) {
            400 -> when (throwable.errorCode) {
                ErrorMessage.ADD_TO_BASKET_CODE -> R.string.error_course_not_found
                ErrorMessage.CHECKOUT_CODE -> R.string.error_payment_not_processed
                ErrorMessage.EXECUTE_ORDER_CODE -> R.string.error_course_not_fullfilled
                else -> R.string.general_error_message
            }
            403 -> when (throwable.errorCode) {
                ErrorMessage.EXECUTE_ORDER_CODE -> R.string.error_course_not_fullfilled
                else -> R.string.error_user_not_authenticated
            }
            406 -> R.string.error_course_already_paid
            else -> R.string.general_error_message
        }
    } else {
        R.string.general_error_message
    }

    private fun startLoading() {
        _showLoader.value = true
    }

    fun endLoading() {
        _showLoader.value = false
    }

    fun setPurchaseToken(purchaseToken: String) {
        this.purchaseToken = purchaseToken
    }

    fun showFullScreenLoader() {
        _displayFullscreenLoaderDialog.value = true
    }

    fun fullScreenLoaderShown() {
        _displayFullscreenLoaderDialog.value = false
    }

    private fun refreshCourseData() {
        _refreshCourseData.postValue(true)
    }

    fun courseRefreshed() {
        _refreshCourseData.value = false
    }

    fun processComplete() {
        _processComplete.postValue(true)
    }

    fun reset() {
        _isVerificationPending = true
        _displayFullscreenLoaderDialog.value = false
        _processComplete.value = false
    }
}
