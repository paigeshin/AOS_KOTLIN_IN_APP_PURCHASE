package com.paigesoftware.kotlininapppurchase.consumer

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

class BillingClientSetup private constructor() {

    companion object {

        @Volatile
        private var INSTANCE: BillingClient? = null

        fun getInstance(
            context: Context,
            purchasesUpdatedListener: PurchasesUpdatedListener
        ): BillingClient {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = BillingClient.newBuilder(context)
                    .enablePendingPurchases()
                    .setListener(purchasesUpdatedListener)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }

}
