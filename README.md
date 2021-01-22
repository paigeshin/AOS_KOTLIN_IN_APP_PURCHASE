### NotionLink
[Notion Doc](https://www.notion.so/In-App-Purchase-493c02d3abbe47449bb13e2868f68354)

❗️현재 internal test로 돌리고 있는데 이틀정도 지나야 심사가 끝난다고 한다. 그때 아래 코드가 되는지 안되는지 확인 가능.

ℹ️ product id 는 반드시 소문자여야한다.

1. Set Up Marchant Account 
2. Create Application 
3. Create Product or Subscription
4. Get Product id or Subscription id
5. Release your app for Internal Testing
6. Test it 
7. Apply it into real application

### Internal Test

![https://s3-us-west-2.amazonaws.com/secure.notion-static.com/4ece8674-bb26-4997-8a72-b55f006855b0/Untitled.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/4ece8674-bb26-4997-8a72-b55f006855b0/Untitled.png)

# Dependency - App Level

```kotlin
def billing_version = "3.0.2"

implementation "com.android.billingclient:billing:$billing_version"
```

# Manifest

```kotlin
<uses-permission android:name="com.android.vending.BILLING" />
```

# Simple Way to build in app purchase

- SimpleProuductActivity

```kotlin
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
```

# In App Purchase with RecyclerView

### InApp Purchase

- Singleton class which returns BillingClient

```kotlin
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
```

- ProductsAdapter

```kotlin
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
```

- ConsumerPurchaseItemActivity

```kotlin
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
```

### Subscription

- singleton

```kotlin

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

```

- ProductsAdapter

```kotlin
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

```

- SubscriptionActivity

```kotlin
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
```