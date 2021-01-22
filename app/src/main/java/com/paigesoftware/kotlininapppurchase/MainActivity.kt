package com.paigesoftware.kotlininapppurchase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.paigesoftware.kotlininapppurchase.consumer.ConsumerPurchaseItemActivity
import com.paigesoftware.kotlininapppurchase.consumer.SubscriptionActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_go_to_simple_product.setOnClickListener(this)
        button_go_to_consumer.setOnClickListener(this)
        button_go_to_subscription.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.button_go_to_simple_product -> {
                startActivity(Intent(this, SimpleProductActivity::class.java))
            }
            R.id.button_go_to_consumer -> {
                startActivity(Intent(this, ConsumerPurchaseItemActivity::class.java))
            }
            R.id.button_go_to_subscription -> {
                startActivity(Intent(this, SubscriptionActivity::class.java))
            }
        }
    }
}