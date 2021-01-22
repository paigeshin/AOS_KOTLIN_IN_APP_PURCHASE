package com.paigesoftware.kotlininapppurchase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.billingclient.api.*
import kotlinx.android.synthetic.main.activity_simple_product.*

class SimpleProductActivity : AppCompatActivity(), PurchasesUpdatedListener {

    companion object {
        //productId
        private const val SKU = "productid"
        //another productId
        private const val ANOTHER_SKU = "productid2"
    }

    private lateinit var mBillingClient: BillingClient
    private val mSkuList = ArrayList<String>()
    private var mSkuDetails: SkuDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_product)

        title = "인앱 구매"

        //in-app purchase가 enable됬을 때만 구매 버튼을 보여준다.
        buttonBuy.visibility = View.GONE
        buttonBuy.isEnabled = false

        mSkuList.add(SKU)
        setUpBillingClient()

    }

    private fun setUpBillingClient() {
        mBillingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build()
        mBillingClient.startConnection(object: BillingClientStateListener{
            override fun onBillingSetupFinished(billingSetUpResult: BillingResult) {
                when(billingSetUpResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        if(mBillingClient.isReady) {
                            loadAllSkus()
                        } else {
                            Toast.makeText(this@SimpleProductActivity, "Billing is not yet available, Try again!", Toast.LENGTH_LONG).show()
                        }
                    }
                    BillingClient.BillingResponseCode.ERROR -> {

                    }
                }
            }

            override fun onBillingServiceDisconnected() {

            }

        })
    }

    private fun loadAllSkus() {
        val skuDetailsParams = SkuDetailsParams.newBuilder()
                .setSkusList(mSkuList)
                .setType(BillingClient.SkuType.INAPP) //.setType(BillingClient.SkuType.SUBS), for subscription
                .build()

        mBillingClient.querySkuDetailsAsync(skuDetailsParams, SkuDetailsResponseListener { queryBillingResult, skuDetailsList ->
            if(queryBillingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                for(skuDetails in skuDetailsList) {
                    if(skuDetails is SkuDetails) {
                        if(skuDetails.sku == SKU) {
                            mSkuDetails = skuDetails

                            /* query purchases, 유저 history 가져오기  */
//                            val purchaseHistory = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP)
//                            purchaseHistory.responseCode
//                            purchaseHistory.purchasesList
//                            purchaseHistory.billingResult

                            /* query purchases, 유저 구매내역 가져오기  */
                            val purchaseResultList = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP).purchasesList
                            var isOwned = false
                            purchaseResultList?.let {
                                for(purchaseResult in it) {
                                    val skuResult = purchaseResult.sku
                                    if(skuResult == SKU) {
                                        isOwned = true
                                        Toast.makeText(this@SimpleProductActivity, "You are a premium user", Toast.LENGTH_LONG).show()
                                        buttonBuy.visibility = View.GONE
                                        break
                                    }
                                }
                            }
                            if(isOwned) {
                                 return@SkuDetailsResponseListener
                            } //이미 유저가 프러덕트를 구매했으면 아래 로직을 실행하지 않는다.


                            /* get price and display it */
                            val price = skuDetails.price
                            buttonBuy.text = "Buy $price${skuDetails.priceCurrencyCode}"

                            //in-app purchase가 enable됬을 때만 구매 버튼을 보여준다.
                            buttonBuy.visibility = View.VISIBLE
                            buttonBuy.isEnabled = true
                            buttonBuy.setOnClickListener {
                                val billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build()
                                mBillingClient.launchBillingFlow(this@SimpleProductActivity, billingFlowParams)
                            }
                        }
                        //다른 프러덕트 핸들링
                        else if (skuDetails.sku == ANOTHER_SKU) {

                        }
                    }
                }
            }
        })
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        val responseCode = billingResult.responseCode
        if(responseCode == BillingClient.BillingResponseCode.OK && !purchaseList.isNullOrEmpty()) {
            for(purchase in purchaseList) {
                handlePurchase(purchase)
            }
        }

    }

    private fun handlePurchase(purchase: Purchase) {
        if(purchase.sku == SKU) {

            //get user purchase history and purchases using these methods
//            mBillingClient.queryPurchases("")
//            mBillingClient.queryPurchaseHistoryAsync("")

            when(purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if(purchase.isAcknowledged) {
                        Toast.makeText(this@SimpleProductActivity, "Purchase done, Enjoy your product.", Toast.LENGTH_LONG).show()
                    } else {
                        //Acknowledge를 강제하는 코드다.
                        /* Crucial, Acknowledgement makes refund impossible, when three days are passed */
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                        mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, AcknowledgePurchaseResponseListener { acknowledgeBillingPurchaseResult ->
                            val isAcknowledged = acknowledgeBillingPurchaseResult.responseCode == BillingClient.BillingResponseCode.OK
                            if(isAcknowledged) {
                                Toast.makeText(this@SimpleProductActivity, "Purchase Acknowledged.", Toast.LENGTH_LONG).show()
                            }
                        })
                    }

                }
                Purchase.PurchaseState.PENDING -> {

                }
                Purchase.PurchaseState.UNSPECIFIED_STATE -> {

                }
            }
        }
    }




}