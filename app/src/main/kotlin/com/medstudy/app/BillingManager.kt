package com.medstudy.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing for the one monthly subscription. The app never decides what a purchase is worth:
 * it only forwards the purchase token to the backend (verify-purchase), which checks it with Google and
 * writes the paid period. The app then re-asks the server for the entitlement.
 */
class BillingManager(
    context: Context,
    private val account: SupabaseAccount,
    private val onPurchaseRecorded: suspend () -> Unit,
) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()
    private var details: ProductDetails? = null

    private val mutablePrice = MutableStateFlow<String?>(null)
    /** Play's localized price for the monthly plan, e.g. "₹199.00", once loaded. */
    val price: StateFlow<String?> = mutablePrice.asStateFlow()

    private val mutableMessage = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = mutableMessage.asStateFlow()

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        val ready = CompletableDeferred<Boolean>()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) { ready.complete(result.responseCode == BillingClient.BillingResponseCode.OK) }
            override fun onBillingServiceDisconnected() { ready.complete(false) }
        })
        return ready.await()
    }

    /** Loads the product and its price. Returns false when Google Play is unavailable (for example a plain emulator). */
    suspend fun load(): Boolean {
        if (!connect()) { mutableMessage.value = "Google Play isn't available on this device."; return false }
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build()),
        ).build()
        val result = client.queryProductDetails(params)
        details = result.productDetailsList?.firstOrNull()
        mutablePrice.value = details?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        if (details == null) mutableMessage.value = "The subscription isn't available yet."
        return details != null
    }

    fun launch(activity: Activity) {
        scope.launch {
            if (details == null && !load()) return@launch
            val product = details ?: return@launch
            val offer = product.subscriptionOfferDetails?.firstOrNull() ?: run { mutableMessage.value = "No offer is available."; return@launch }
            val flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.offerToken).build()),
                )
                .setObfuscatedAccountId(account.userId) // ties the purchase to this account on Google's side too
                .build()
            val result = client.launchBillingFlow(activity, flow)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) mutableMessage.value = "Couldn't start the purchase."
        }
    }

    /** Re-sends any purchase Google already knows about (new phone, reinstall) to the backend. */
    suspend fun restore(): Boolean {
        if (!connect()) { mutableMessage.value = "Google Play isn't available on this device."; return false }
        val purchases = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        val active = purchases.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        active.forEach { record(it) }
        if (active.isEmpty()) mutableMessage.value = "No purchase to restore on this Google account."
        return active.isNotEmpty()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { scope.launch { record(it) } }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> mutableMessage.value = "The purchase didn't go through."
        }
    }

    private suspend fun record(purchase: Purchase) {
        runCatching { account.verifyPurchase(PRODUCT_ID, purchase.purchaseToken) }
            .onSuccess {
                // Acknowledge on the device too; Play refunds unacknowledged purchases after 3 days. Safe to repeat.
                if (!purchase.isAcknowledged) {
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) {}
                }
                onPurchaseRecorded()
            }
            .onFailure { mutableMessage.value = "We couldn't confirm your purchase yet. It will be retried; tap Restore if it doesn't unlock." }
    }

    fun clearMessage() { mutableMessage.value = null }

    companion object {
        /** Must match the subscription's product id in Play Console. */
        const val PRODUCT_ID = "medstudy_monthly"
    }
}
