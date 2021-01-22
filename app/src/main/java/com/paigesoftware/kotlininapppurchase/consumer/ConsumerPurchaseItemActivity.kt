package com.paigesoftware.kotlininapppurchase.consumer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.paigesoftware.kotlininapppurchase.R
import kotlinx.android.synthetic.main.activity_consumer_purchase_item.*
import java.lang.StringBuilder

class ConsumerPurchaseItemActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var mBillingClient: BillingClient
    private lateinit var mConsumerResponseListener: ConsumeResponseListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consumer_purchase_item)

        title = "컨슈머"
        setUpBillingClient()
        setRecyclerView()

    }

    private fun setRecyclerView() {
        recyclerview_consumer.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        recyclerview_consumer.layoutManager = layoutManager
        recyclerview_consumer.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        buttonShowItems.setOnClickListener {
            if(mBillingClient.isReady) {
                val skuDetailsParams = SkuDetailsParams.newBuilder()
                    .setSkusList(arrayListOf("my_product_jewel_of_item", "my_product_sword_of_angle"))
                    .setType(BillingClient.SkuType.INAPP)
                    .build()
                mBillingClient.querySkuDetailsAsync(skuDetailsParams
                ) { skuDetailsBillingResult, skuDetailsList ->
                    if(skuDetailsBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        loadproductToRecyclerView(skuDetailsList)
                    } else {
                        Toast.makeText(this@ConsumerPurchaseItemActivity, "Error code: ${skuDetailsBillingResult.responseCode}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun loadproductToRecyclerView(skuDetailsList: List<SkuDetails>?) {
        skuDetailsList?.let {
            val adapter = ProductAdapter(this, it, mBillingClient)
            recyclerview_consumer.adapter = adapter
        }
    }

    private fun setUpBillingClient() {
        mConsumerResponseListener = ConsumeResponseListener { consumeBillingResult, p1 ->
            if(consumeBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Toast.makeText(this@ConsumerPurchaseItemActivity, "Consume OK!", Toast.LENGTH_LONG).show()
            }
        }
        mBillingClient = BillingClientSetup.getInstance(this, this)
        mBillingClient.startConnection(object: BillingClientStateListener{
            override fun onBillingSetupFinished(stateBillingResult: BillingResult) {
                if(stateBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(this@ConsumerPurchaseItemActivity, "Success to connect billing!", Toast.LENGTH_LONG).show()
                    //Query
                    val purchaseList = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP).purchasesList
                    handleItemAlreadyPurchased(purchaseList)
                } else {
                    Toast.makeText(this@ConsumerPurchaseItemActivity, "Error code: ${stateBillingResult.responseCode}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(this@ConsumerPurchaseItemActivity, "You are disconnect from Billing Service", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handleItemAlreadyPurchased(purchaseList: List<Purchase>?) {
        val stringBuilderPurchasedItemList = StringBuilder(textview_consume.text.toString())
        purchaseList?.let {
            for(purchase in purchaseList) {
                if(purchase.sku == "my_product_jewel_of_item") {
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    mBillingClient.consumeAsync(consumeParams, mConsumerResponseListener)
                }
                stringBuilderPurchasedItemList
                    .append("\n${purchase.sku}")
                    .append("\n")
            }
        }
        textview_consume.text = stringBuilderPurchasedItemList.toString()
        textview_consume.visibility = View.VISIBLE
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        if(billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchaseList.isNullOrEmpty()) {
            handleItemAlreadyPurchased(purchaseList)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this@ConsumerPurchaseItemActivity, "User cancelled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@ConsumerPurchaseItemActivity, "Error: ${billingResult.responseCode}", Toast.LENGTH_LONG).show()
        }
    }


}