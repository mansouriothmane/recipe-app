package com.enseirb.myreceipts.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enseirb.myreceipts.*
import com.enseirb.myreceipts.activity.MainActivity
import com.enseirb.myreceipts.activity.MealActivity
import com.enseirb.myreceipts.activity.ReceiptActivity
import com.enseirb.myreceipts.ui.IngredientsAdapter
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL


class CategoryService {

     fun getCategories(recyclerView: RecyclerView, applicationContext: Context, activity: MainActivity){
        val url = URL("https://www.themealdb.com/api/json/v1/1/categories.php")

        var categoriesAdapter: CategoriesAdapter

        val request = Request.Builder().url(url).build();

        val client = OkHttpClient();

        client.newCall(request).enqueue( object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.localizedMessage?.let { Log.e("OKHTTP", it) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val gson = Gson()
                    val categoryResponse = gson.fromJson(it, CategoryResponse::class.java)
                    categoryResponse.categories?.let { it1 ->
                        activity.runOnUiThread {
                            categoriesAdapter = CategoriesAdapter(it1, applicationContext)
                            recyclerView.adapter = categoriesAdapter
                            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
                        }
                    }
                }
            }
        })
    }

    fun getDetailsOfCategory(strCategory: String ,recyclerView: RecyclerView, applicationContext:  Context, activity: MealActivity){
        val url = URL("https://www.themealdb.com/api/json/v1/1/filter.php?c=$strCategory")

        var mealsAdapter: MealsAdapter
        val request = Request.Builder().url(url).build();

        val client = OkHttpClient();

        client.newCall(request).enqueue( object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.localizedMessage?.let { Log.e("OKHTTP", it) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val gson = Gson()
                    val mealResponse = gson.fromJson(it, MealResponse::class.java)
                    mealResponse.meals?.let { it1 ->
                        activity.runOnUiThread {
                            mealsAdapter = MealsAdapter(it1, applicationContext)
                            recyclerView.adapter = mealsAdapter
                            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
                        }
                    }
                }
            }
        })
    }

    private fun getIngredientsMeasures(responseBody : String?, receiptResponse: ReceiptResponse){
        val returnedRecipe = responseBody?.let { JSONObject(it) }?.getJSONArray("meals")?.getJSONObject(0)
        for(j in 1 until 21) {
            if (returnedRecipe != null) {
                if (returnedRecipe.getString("strIngredient$j").trim()
                        .isNotEmpty() && returnedRecipe.getString("strIngredient$j")
                        .trim() != "null"
                    && returnedRecipe.getString("strMeasure$j").trim()
                        .isNotEmpty() && returnedRecipe.getString("strMeasure$j")
                        .trim() != "null"
                ) {
                    receiptResponse.receipts?.get(0)?.strIngredient?.add(
                        returnedRecipe.getString(
                            "strIngredient$j"
                        )
                    )
                    receiptResponse.receipts?.get(0)?.strMeasure?.add(
                        returnedRecipe.getString(
                            "strMeasure$j"
                        )
                    )
                }
            }
        }
    }

    fun getReceiptsOfMeal(idMeal: String, titleView: TextView, thumbView: ImageView, ingredientsView: RecyclerView, instructionsView: TextView, youtubeButton: Button, applicationContext: Context, activity: ReceiptActivity){

        val url = URL("https://www.themealdb.com/api/json/v1/1/lookup.php?i=$idMeal")

        var ingredientsAdapter : IngredientsAdapter
        val request = Request.Builder().url(url).build();

        val client = OkHttpClient();

        client.newCall(request).enqueue( object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.localizedMessage?.let { Log.e("OKHTTP", it) }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                responseBody.let {
                    val gson = Gson()
                    val receiptResponse = gson.fromJson(it, ReceiptResponse::class.java)
                    getIngredientsMeasures(responseBody, receiptResponse)
                    receiptResponse.receipts?.let { it1 ->
                        activity.runOnUiThread {

                            titleView.setText(it1.get(0).strMeal)
                            Picasso.get().load(it1.get(0).strMealThumb).into(thumbView)
                            instructionsView.setText(it1.get(0).strInstructions)
                            ingredientsAdapter = IngredientsAdapter(it1.get(0), applicationContext)
                            ingredientsView.adapter = ingredientsAdapter
                            ingredientsView.layoutManager = LinearLayoutManager(applicationContext)

                            youtubeButton.setOnClickListener {
                                val youtubeUrl = Uri.parse(it1.get(0).strYoutube)
                                if(youtubeUrl == null){
                                    youtubeButton.isEnabled = false
                                    youtubeButton.alpha = 0.5f
                                }
                                else {
                                    val youtubeIntent = Intent(Intent.ACTION_VIEW, youtubeUrl)
                                    youtubeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    applicationContext.startActivity(youtubeIntent)
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}
