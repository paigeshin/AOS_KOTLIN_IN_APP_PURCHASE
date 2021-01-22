package com.paigesoftware.kotlininapppurchase.consumer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import com.paigesoftware.kotlininapppurchase.R
import kotlinx.android.synthetic.main.item_product_display.view.*

class ProductAdapter(
    private val activity: AppCompatActivity,
    private val skuDetailsList: List<SkuDetails>,
    private val billingClient: BillingClient
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {



    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(activity.baseContext)
            .inflate(R.layout.item_product_display, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.itemView.textview_product_name.text = skuDetailsList[position].title
        holder.itemView.textview_product_price.text = skuDetailsList[position].price
        holder.itemView.textview_product_description.text = skuDetailsList[position].description

        holder.itemView.setOnClickListener {
            //launch billing flow
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetailsList[position])
                .build()
            val response = billingClient
                .launchBillingFlow(activity, billingFlowParams)
                .responseCode
            when(response) {
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                    Toast.makeText(activity, "BILLING_UNAVAILABLE", Toast.LENGTH_LONG).show()
                }
                BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                    Toast.makeText(activity, "DEVELOPER_ERROR", Toast.LENGTH_LONG).show()
                }
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                    Toast.makeText(activity, "FEATURE_NOT_SUPPORTED", Toast.LENGTH_LONG).show()
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    Toast.makeText(activity, "ITEM_ALREADY_OWNED", Toast.LENGTH_LONG).show()
                }
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                    Toast.makeText(activity, "SERVICE_DISCONNECTED", Toast.LENGTH_LONG).show()
                }
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                    Toast.makeText(activity, "SERVICE_TIMEOUT", Toast.LENGTH_LONG).show()
                }
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                    Toast.makeText(activity, "ITEM_UNAVAILABLE", Toast.LENGTH_LONG).show()
                }

            }
        }

    }

    override fun getItemCount(): Int {
        return skuDetailsList.size
    }
}