package com.karthi.awsamplify

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import android.R.attr.description
import android.util.Log
import com.amazonaws.amplify.generated.graphql.CreateTodoMutation
import com.amazonaws.amplify.generated.graphql.GetTodoQuery
import com.amazonaws.amplify.generated.graphql.ListTodosQuery
import type.CreateTodoInput
import com.apollographql.apollo.exception.ApolloException
import javax.annotation.Nonnull
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response


class MainActivity : AppCompatActivity() {

    var mAWSAppSyncClient: AWSAppSyncClient? = null

    private val mutationCallback = object : GraphQLCall.Callback<CreateTodoMutation.Data>() {

        override fun onResponse(response: Response<CreateTodoMutation.Data>) {
            Log.i("mut-Callback-Results", "Added Todo")
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", e.toString())
        }
    }

    private val queryCallback = object : GraphQLCall.Callback<GetTodoQuery.Data>() {

        override fun onResponse(response: Response<GetTodoQuery.Data>) {
            Log.d("queryCallback-Results", response.data().toString())
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", e.toString())
        }
    }

    private val listCallback = object : GraphQLCall.Callback<ListTodosQuery.Data>() {

        override fun onResponse(response: Response<ListTodosQuery.Data>) {
            Log.d("listCallback-Results", response.data().toString())
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", e.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAWSAppSyncClient = AWSAppSyncClient.builder()
            .context(applicationContext)
            .awsConfiguration(AWSConfiguration(applicationContext))
            .build()

        runMutation()
    }

    fun runMutation() {

        val createTodoInput = CreateTodoInput.builder().name("Use AppSync").description("Realtime and Offline").phNo("9715460405").build()

        mAWSAppSyncClient?.mutate(CreateTodoMutation.builder().input(createTodoInput).build())
            ?.enqueue(mutationCallback)

        val getTodoInput = GetTodoQuery.builder().id("3bdbc22d-2e67-4a91-b90c-c5a11bf8175c").build()
        mAWSAppSyncClient?.query(getTodoInput)?.enqueue(queryCallback)

        val listTodoInput = ListTodosQuery.builder().build()
        mAWSAppSyncClient?.query(listTodoInput)?.enqueue(listCallback)
    }
}
