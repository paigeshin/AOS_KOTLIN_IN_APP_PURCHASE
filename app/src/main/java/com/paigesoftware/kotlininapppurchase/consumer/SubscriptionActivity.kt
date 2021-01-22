package com.paigesoftware.kotlininapppurchase.consumer


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.paigesoftware.kotlininapppurchase.R
import kotlinx.android.synthetic.main.activity_subscription.*


class SubscriptionActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var mBillingClient: BillingClient
    private lateinit var mAcknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        title = "구독"
        setUpBillingClient()
        setRecyclerView()

    }

    private fun setRecyclerView() {
        recyclerview_subscribe.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        recyclerview_subscribe.layoutManager = layoutManager
        recyclerview_subscribe.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))

        if(mBillingClient.isReady) {
            val skuDetailsParams = SkuDetailsParams.newBuilder()
                .setSkusList(arrayListOf("my_product_subscription"))
                .setType(BillingClient.SkuType.SUBS)
                .build()
            mBillingClient.querySkuDetailsAsync(skuDetailsParams
            ) { skuDetailsBillingResult, skuDetailsList ->
                if(skuDetailsBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadproductToRecyclerView(skuDetailsList)
                } else {
                    Toast.makeText(this@SubscriptionActivity, "Error code: ${skuDetailsBillingResult.responseCode}", Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    private fun loadproductToRecyclerView(skuDetailsList: List<SkuDetails>?) {
        skuDetailsList?.let {
            val adapter = ProductAdapter(this, it, mBillingClient)
            recyclerview_subscribe.adapter = adapter
        }
    }

    private fun setUpBillingClient() {
        mAcknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { acknowledgePurchaseBillingResult ->
            if(acknowledgePurchaseBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                textview_subscribe.visibility = View.VISIBLE
            }
        }
        mBillingClient = BillingClientSetup.getInstance(this, this)
        mBillingClient.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(stateBillingResult: BillingResult) {
                if(stateBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(this@SubscriptionActivity, "Success to connect billing!", Toast.LENGTH_LONG).show()
                    //Query
                    val purchaseList = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS).purchasesList
                    if(!purchaseList.isNullOrEmpty()) {
                        recyclerview_subscribe.visibility = View.GONE
                        for(purchase in purchaseList) {
                            handleItemAlreadyPurchased(purchase)
                        }
                    } else {
                        textview_subscribe.visibility = View.GONE
                        recyclerview_subscribe.visibility = View.VISIBLE
                        loadAllSubscribePackage()
                    }
                } else {
                    Toast.makeText(this@SubscriptionActivity, "Error code: ${stateBillingResult.responseCode}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(this@SubscriptionActivity, "You are disconnect from Billing Service", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadAllSubscribePackage() {
        if(mBillingClient.isReady) {
            val skuDetailsParams = SkuDetailsParams.newBuilder()
                    .setSkusList(arrayListOf("vvip_2020"))
                    .setType(BillingClient.SkuType.SUBS)
                    .build()
            mBillingClient.querySkuDetailsAsync(skuDetailsParams) { queryBillingResult, skuDetailsList ->
                if (queryBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    skuDetailsList?.let {
                        val adapter = ProductAdapter(this@SubscriptionActivity, it, mBillingClient)
                        recyclerview_subscribe.adapter = adapter
                    }
                } else {
                    Toast.makeText(this@SubscriptionActivity, "Error: ${queryBillingResult.responseCode}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this@SubscriptionActivity, "Billing Client not ready!", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleItemAlreadyPurchased(purchase: Purchase) {
        if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if(!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, mAcknowledgePurchaseResponseListener)
            } else {
                textview_subscribe.visibility = View.VISIBLE
                textview_subscribe.text = "You are Premium User!!!"
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        if(billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchaseList.isNullOrEmpty()) {
            for(purchase in purchaseList) {
                handleItemAlreadyPurchased(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this@SubscriptionActivity, "User cancelled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@SubscriptionActivity, "Error: ${billingResult.responseCode}", Toast.LENGTH_LONG).show()
        }
    }

}