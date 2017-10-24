package com.alikazi.cc_airtasker.main;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alikazi.cc_airtasker.R;
import com.alikazi.cc_airtasker.conf.AppConf;
import com.alikazi.cc_airtasker.conf.NetConstants;
import com.alikazi.cc_airtasker.db.AppDatabase;
import com.alikazi.cc_airtasker.db.DbHelper;
import com.alikazi.cc_airtasker.db.FakeDataDb;
import com.alikazi.cc_airtasker.db.entities.FeedEntity;
import com.alikazi.cc_airtasker.db.entities.ProfileEntity;
import com.alikazi.cc_airtasker.db.entities.TaskEntity;
import com.alikazi.cc_airtasker.network.NetworkProcessor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NetworkProcessor.FeedRequestListener,
        NetworkProcessor.TasksRequestListener,
        NetworkProcessor.ProfileRequestListener,
        FakeDataDb.FakeDbCallbacksListener {

    private static final String LOG_TAG = AppConf.LOG_TAG_CC_AIRTASKER;

    private static final int SNACKBAR_FEED = 0;
    private static final int SNACKBAR_TASKS = 1;
    private static final int SNACKBAR_PROFILE = 2;
    private static final int SNACKBAR_DONE = 3;
    private static final int SNACKBAR_REQUEST_ERROR = 4;
    private static final int SNACKBAR_NO_INTERNET = 5;

    // Logic
    private ArrayList<FeedEntity> mFeed;
    private ArrayList<TaskEntity> mTasks;
    private ArrayList<ProfileEntity> mProfiles;
    private LinkedHashSet<Integer> mTaskIds;
    private LinkedHashSet<Integer> mProfileIds;
    private FeedAdapter mFeedAdapter;
    private NetworkProcessor mNetworkProcessor;
    private AppDatabase mDbInstance;

    // UI
    private RecyclerView mRecyclerView;
    private TextView mEmptyListTextView;
    private ProgressBar mProgressBar;
    private FloatingActionButton mFab;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Snackbar mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUi();
        mDbInstance = AppDatabase.getDatabaseInstance(this, true);
        DbHelper.clearDbOnInit(mDbInstance);
        mNetworkProcessor = new NetworkProcessor(this, mDbInstance,
                this,this, this);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestFeedFromServer(true);
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestFeedFromServer(false);
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mFeedAdapter = new FeedAdapter(this);
        mRecyclerView.setAdapter(mFeedAdapter);
        setupRecyclerScrollListener();
        showHideEmptyListMessage(true);
//        FakeDataDb.initDbFakeDataAsync(mDbInstance, this);
    }

    private void initUi() {
        mFab = findViewById(R.id.main_fab);
        mRecyclerView = findViewById(R.id.main_recycler_view);
        mProgressBar = findViewById(R.id.main_progress_bar);
        mEmptyListTextView = findViewById(R.id.main_empty_list_message);
        mSwipeRefreshLayout = findViewById(R.id.main_swipe_refresh_layout);
    }

    @Override
    public void onFakeDbCreationComplete() {
        fetchFakeDbData();
    }

    private void fetchFakeDbData() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                List<FeedEntity> feedEntities = mDbInstance.feedModel().loadAllFeed();
                for (FeedEntity feedEntity : feedEntities) {
                    Log.d(LOG_TAG, "feedEntity.id: " + feedEntity.id);
                    Log.d(LOG_TAG, "feedEntity.task_id: " + feedEntity.task_id);
                    Log.d(LOG_TAG, "feedEntity.profile_id: " + feedEntity.profile_id);
                    Log.d(LOG_TAG, "feedEntity.event: " + feedEntity.event);
                    Log.d(LOG_TAG, "feedEntity.created_at: " + feedEntity.created_at);
                    Log.d(LOG_TAG, "feedEntity.text: " + feedEntity.text);
                    Log.d(LOG_TAG, "-----------------------------------------------------");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null &&
                connectivityManager.getActiveNetworkInfo() != null &&
                connectivityManager.getActiveNetworkInfo().isConnected();
    }

    private void showHideEmptyListMessage(boolean show) {
        if (mEmptyListTextView != null) {
            mEmptyListTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showHideProgressBar(boolean show) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showHideSwipeRefreshing(boolean show) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(show);
        }
    }

    private void processSnackbar(int id) {
        if (mSnackbar == null) {
            mSnackbar = Snackbar.make(mSwipeRefreshLayout, "", Snackbar.LENGTH_INDEFINITE);
        }
        switch (id) {
            case SNACKBAR_FEED:
                mSnackbar.setText(R.string.snackbar_message_getting_feed);
                mSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
                break;
            case SNACKBAR_TASKS:
                mSnackbar.setText(R.string.snackbar_message_processing_tasks);
                mSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
                break;
            case SNACKBAR_PROFILE:
                mSnackbar.setText(R.string.snackbar_message_processing_profiles);
                mSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
                break;
            case SNACKBAR_DONE:
                mSnackbar.setText(R.string.snackbar_message_done);
                mSnackbar.setDuration(Snackbar.LENGTH_SHORT);
                break;
            case SNACKBAR_REQUEST_ERROR:
                mSnackbar.setText(R.string.snackbar_message_feed_response_error);
                mSnackbar.setDuration(Snackbar.LENGTH_LONG);
                break;
            case SNACKBAR_NO_INTERNET:
                mSnackbar.setText(R.string.snackbar_message_no_internet);
                mSnackbar.setDuration(Snackbar.LENGTH_LONG);
            default:
                mSnackbar.dismiss();
        }
        mSnackbar.show();
    }

    private void setupRecyclerScrollListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && mFab.isShown()) {
                    mFab.hide();
                } else {
                    mFab.show();
                }
            }
        });
    }

    private void requestFeedFromServer(boolean showProgressBar) {
        Log.i(LOG_TAG, "requestFeedFromServer");
        if (mNetworkProcessor != null && isInternetConnected()) {
            showHideProgressBar(showProgressBar);
            showHideSwipeRefreshing(!showProgressBar);
            showHideEmptyListMessage(false);
            processSnackbar(SNACKBAR_FEED);
            mNetworkProcessor.getFeed();
        } else {
            processSnackbar(SNACKBAR_NO_INTERNET);
        }
    }

    @Override
    public void onFeedRequestSuccess() {
        Log.i(LOG_TAG, "onFeedRequestSuccess");
        mFeed = new ArrayList<>();
        mFeed = (ArrayList<FeedEntity>) mDbInstance.feedModel().loadAllFeed();
        processTaskAndProfileIds();
    }

    @Override
    public void onFeedRequestFailure() {
        Log.i(LOG_TAG, "onFeedRequestFailure");
        processDefaultRequestFailure();
    }

    private void processTaskAndProfileIds() {
        mTaskIds = new LinkedHashSet<>();
        mProfileIds = new LinkedHashSet<>();
        mTaskIds.clear();
        mProfileIds.clear();
        for (FeedEntity feedItem : mFeed) {
            mTaskIds.add(feedItem.task_id);
            mProfileIds.add(feedItem.profile_id);
        }

        requestTasksFromServer(mTaskIds);
    }

    private void requestTasksFromServer(LinkedHashSet<Integer> taskIds) {
        Log.i(LOG_TAG, "requestFeedFromServer");
        if (mNetworkProcessor != null) {
            processSnackbar(SNACKBAR_TASKS);
            mNetworkProcessor.getTasks(taskIds);
        }
    }

    @Override
    public void onTasksRequestSuccess() {
        Log.i(LOG_TAG, "onTasksRequestSuccess");
        mTasks = new ArrayList<>();
        mTasks = (ArrayList<TaskEntity>) mDbInstance.taskModel().loadAllTasks();
        requestProfilesFromServer(mProfileIds);
    }

    @Override
    public void onTasksRequestFailure() {
        Log.i(LOG_TAG, "onTasksRequestFailure");
        processDefaultRequestFailure();
    }

    private void requestProfilesFromServer(LinkedHashSet<Integer> profileIds) {
        Log.i(LOG_TAG, "requestProfilesFromServer");
        if (mNetworkProcessor != null) {
            processSnackbar(SNACKBAR_PROFILE);
            mNetworkProcessor.getProfiles(profileIds);
        }
    }

    @Override
    public void onProfilesRequestsSuccess() {
        Log.i(LOG_TAG, "onProfilesRequestsSuccess");
        showHideProgressBar(false);
        showHideSwipeRefreshing(false);
        showHideEmptyListMessage(false);
        processSnackbar(SNACKBAR_DONE);
        mProfiles = new ArrayList<>();
        mProfiles = (ArrayList<ProfileEntity>) mDbInstance.profileModel().loadAllProfiles();
        processFeedWithTasksAndProfiles();
        mFeedAdapter.setFeedList(mFeed);
    }

    @Override
    public void onProfilesRequestFailure() {
        Log.i(LOG_TAG, "onProfilesRequestFailure");
        processDefaultRequestFailure();
    }

    private void processDefaultRequestFailure() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEmptyListTextView.setText(R.string.main_empty_list_message_error);
                showHideProgressBar(false);
                showHideSwipeRefreshing(false);
                showHideEmptyListMessage(true);
                processSnackbar(SNACKBAR_REQUEST_ERROR);
            }
        });
    }

    private void processFeedWithTasksAndProfiles() {
        for (FeedEntity feed : mFeed) {
            // Convert ISO date to Java date
            try {
                SimpleDateFormat isoDateFormat = new SimpleDateFormat(AppConf.DATE_FORMAT_ISO, Locale.US);
//                feed.createdAtJavaDate = isoDateFormat.parse(feed.created_at);
            } catch (Exception e) {
                Log.d(LOG_TAG, "Exception parsing iso date: " + e.toString());
            }

            for (TaskEntity task : mTasks){
                if (feed.task_id == task.id) {

                    // Replace {task_name} with data from task object
                    /*String feedText = feed.text;
                    feedText = feedText.replace(NetConstants.JSON_KEY_TASK_NAME, task.name);
                    feed.processedText = feedText;

                    // Set transient task object on feed
                    feed.task = task;*/
                }
            }

            for (ProfileEntity profile : mProfiles) {

                // Convert mini url of profile photo to full url
                Uri.Builder uriBuilder = new Uri.Builder()
                        .scheme(NetConstants.SCHEME_HTTPS)
                        .authority(NetConstants.STAGE_AIRTASKER)
                        .appendPath(NetConstants.ANDROID_CODE_TEST);
//                profile.avatarFullUrl = uriBuilder.build().toString() + profile.avatar_mini_url;

                if (feed.profile_id == profile.id) {

                    // Replace {profile_name} with data from profile object
                    /*String feedText = feed.processedText;
                    feedText = feedText.replace(NetConstants.JSON_KEY_PROFILE_NAME, profile.first_name);
                    feed.processedText = feedText;

                    // Set transient profile object on feed
                    feed.profile = profile;*/
                }
            }
        }
    }
}
