package com.habitrpg.android.habitica.ui.fragments.social.challenges;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.components.AppComponent;
import com.habitrpg.android.habitica.ui.adapter.social.ChallengesListViewAdapter;
import com.habitrpg.android.habitica.ui.fragments.BaseMainFragment;
import com.magicmicky.habitrpgwrapper.lib.models.Challenge;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.language.Where;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.functions.Action0;

public class ChallengeListFragment extends BaseMainFragment implements SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.challenges_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.challenges_refresh_empty)
    SwipeRefreshLayout swipeRefreshEmptyLayout;

    @BindView(R.id.challenges_list)
    RecyclerView recyclerView;

    private ChallengesListViewAdapter challengeAdapter;
    private boolean viewUserChallengesOnly;
    private Action0 refreshCallback;

    public void setViewUserChallengesOnly(boolean only) {
        this.viewUserChallengesOnly = only;
    }

    public void setRefreshingCallback(Action0 refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    public void setObservable(Observable<ArrayList<Challenge>> listObservable) {
        listObservable
                .subscribe(challenges -> {

                    List<Challenge> userChallenges = this.user.getChallengeList();

                    HashSet<String> userChallengesHash = new HashSet<>();

                    for (Challenge userChallenge : userChallenges) {
                        userChallengesHash.add(userChallenge.id);
                    }

                    userChallenges.clear();

                    for (Challenge challenge : challenges) {
                        if (userChallengesHash.contains(challenge.id) && challenge.name != null && !challenge.name.isEmpty()) {
                            challenge.user_id = this.user.getId();
                            userChallenges.add(challenge);
                        } else {
                            challenge.user_id = null;
                        }

                        challenge.async().save();
                    }

                    setRefreshingIfVisible(swipeRefreshLayout, false);
                    setRefreshingIfVisible(swipeRefreshEmptyLayout, false);

                    if (viewUserChallengesOnly) {
                        setChallengeEntries(userChallenges);
                    } else {
                        setChallengeEntries(challenges);
                    }


                }, throwable -> {
                    Log.e("ChallengeListFragment", "", throwable);

                    setRefreshingIfVisible(swipeRefreshLayout, false);
                    setRefreshingIfVisible(swipeRefreshEmptyLayout, false);
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_challengeslist, container, false);
        unbinder = ButterKnife.bind(this, v);

        challengeAdapter = new ChallengesListViewAdapter(viewUserChallengesOnly);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshEmptyLayout.setOnRefreshListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this.activity));
        recyclerView.setAdapter(challengeAdapter);
        if (!viewUserChallengesOnly) {
            this.recyclerView.setBackgroundResource(R.color.white);
        }

        fetchLocalChallenges();
        return v;
    }

    @Override
    public void injectFragment(AppComponent component) {
        component.inject(this);
    }

    @Override
    public void onRefresh() {
        setRefreshingIfVisible(swipeRefreshEmptyLayout, true);
        setRefreshingIfVisible(swipeRefreshLayout, true);

        fetchOnlineChallenges();
    }

    private void setRefreshingIfVisible(SwipeRefreshLayout refreshLayout, boolean state) {
        if (refreshLayout != null && refreshLayout.getVisibility() == View.VISIBLE) {
            refreshLayout.setRefreshing(state);
        }
    }

    private void fetchLocalChallenges() {
        setRefreshingIfVisible(swipeRefreshLayout, true);

        Where<Challenge> query = new Select().from(Challenge.class).where(Condition.column("name").isNotNull());

        if (viewUserChallengesOnly) {
            query = query.and(Condition.column("user_id").is(user.getId()));
        }

        List<Challenge> challenges = query.queryList();

        if (challenges.size() != 0) {
            setChallengeEntries(challenges);
        }

        setRefreshingIfVisible(swipeRefreshLayout, false);

        // load online challenges & save to database
        onRefresh();
    }

    private void setChallengeEntries(List<Challenge> challenges) {
        if (swipeRefreshEmptyLayout == null || swipeRefreshLayout == null) {
            return;
        }
        if (viewUserChallengesOnly && challenges.size() == 0) {
            swipeRefreshEmptyLayout.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.setVisibility(View.GONE);
        } else {
            swipeRefreshEmptyLayout.setRefreshing(false);
            swipeRefreshEmptyLayout.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.VISIBLE);
        }

        challengeAdapter.setChallenges(challenges);
    }

    private void fetchOnlineChallenges() {
        refreshCallback.call();
    }

    public void addItem(Challenge challenge) {
        challengeAdapter.addChallenge(challenge);
    }

    public void updateItem(Challenge challenge) {
        challengeAdapter.replaceChallenge(challenge);
    }

    @Override
    public String customTitle() {
        return getString(R.string.sidebar_challenges);
    }
}
