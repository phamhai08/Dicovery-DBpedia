package com.brine.discovery.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.brine.discovery.R;
import com.brine.discovery.activity.DetailsActivity;
import com.brine.discovery.activity.RecommendActivity;
import com.brine.discovery.adapter.GridViewAdapter;
import com.brine.discovery.adapter.SelectedResultsAdapter;
import com.brine.discovery.message.MessageObserver;
import com.brine.discovery.message.MessageObserverManager;
import com.brine.discovery.model.Recommend;
import com.brine.discovery.util.DbpediaConstant;

import org.json.JSONArray;
import org.json.JSONException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecommendFragment extends Fragment
        implements GridViewAdapter.GridAdapterCallback, View.OnClickListener,
        SelectedResultsAdapter.SelectedAdapterCallback, MessageObserver{
    private final static String TAG = RecommendFragment.class.getCanonicalName();
    public final static String DATA = "data";
    public final static String TOP_TYPE = "top";
    private final static int MAXTOPRESULT = 20;
    private static final int MAXITEM = 4;

    private RelativeLayout mRltSelectedRecommend;
    private RecyclerView mRecycleRecommed;
    private ImageButton mBtnEXSearch;
    private GridView mGridView;

    private List<Recommend> mRecommendDatas;
    private List<Recommend> mSelectedRecommends;
    private GridViewAdapter mGridAdapter;
    private SelectedResultsAdapter mRecommedAdapter;
    private String mResponse;
    private boolean mTopType;

    public RecommendFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mResponse = getArguments().getString(DATA);
        mTopType = getArguments().getBoolean(TOP_TYPE);
        return inflater.inflate(R.layout.fragment_recommendation, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initUI(view);
        init();
        MessageObserverManager.getInstance().addItem(this);
        initDataSelected();

        if(mTopType){
            parserTopResponseData();
        }else{
            parserNormalResponseData();
        }
    }

    public void initDataSelected() {
        List<Recommend> recommends = MessageObserverManager.getInstance().getSelectedRecommendData();
        if(recommends.size() == 0){
            hideSelectedRecommend();
            return;
        }
        showSelectedRecommend();
        for(Recommend recommend : recommends){
            if(!mSelectedRecommends.contains(recommend)){
                mSelectedRecommends.add(recommend);
            }
        }
        mRecommedAdapter.notifyDataSetChanged();
    }

    @Override
    public void updateSelectedItem(Recommend recommend) {
        showSelectedRecommend();
        if(mSelectedRecommends.contains(recommend)){
            showLogAndToast("Item added. Please choice other item!");
            return;
        }
        if(mSelectedRecommends.size() == MAXITEM){
            mSelectedRecommends.remove(mSelectedRecommends.size() - 1);
            mRecommedAdapter.notifyDataSetChanged();
            showLogAndToast("Max item is 4!");
        }
        mSelectedRecommends.add(recommend);
        mRecommedAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MessageObserverManager.getInstance().removeItem(this);
    }

    private void initUI(View view){
        mRltSelectedRecommend = (RelativeLayout) view.findViewById(R.id.rlt_seledted_recommend);
        mRecycleRecommed = (RecyclerView) view.findViewById(R.id.recycle_selected_uri);
        mGridView = (GridView) view.findViewById(R.id.grid_view);
        mBtnEXSearch = (ImageButton) view.findViewById(R.id.btn_EXSearch);
        mBtnEXSearch.setOnClickListener(this);
    }

    private void init(){
        mRecommendDatas = new ArrayList<>();
        mGridAdapter = new GridViewAdapter(getContext(), mRecommendDatas, this);
        mGridView.setAdapter(mGridAdapter);

        mSelectedRecommends = new ArrayList<>();
        mRecommedAdapter = new SelectedResultsAdapter(getContext(), mSelectedRecommends, this);
        mRecycleRecommed.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManagerRecommend =
                new LinearLayoutManager(getContext(),
                        LinearLayoutManager.HORIZONTAL, false);
        mRecycleRecommed.setLayoutManager(layoutManagerRecommend);
        mRecycleRecommed.addItemDecoration(
                new DividerItemDecoration(getContext(), LinearLayout.HORIZONTAL));
        mRecycleRecommed.setItemAnimator(new DefaultItemAnimator());
        mRecycleRecommed.setAdapter(mRecommedAdapter);
    }

    @Override
    public void onClick(final Recommend recommend) {
        new AlertDialog.Builder(getContext())
                .setMessage("Are you sure you want to delete " + recommend.getLabel())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectedRecommends.remove(recommend);
                        mRecommedAdapter.notifyDataSetChanged();
                        if(mSelectedRecommends.isEmpty()){
                            hideSelectedRecommend();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void hideSelectedRecommend(){
        mRltSelectedRecommend.setVisibility(View.GONE);
    }

    private void showSelectedRecommend(){
        mRltSelectedRecommend.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_EXSearch:
                if(mSelectedRecommends.size() == 0){
                    showLogAndToast("Please select uri!");
                    return;
                }
                final List<String> inputUris = convertToListString(mSelectedRecommends);
                ((RecommendActivity)getActivity()).EXSearch(inputUris);
                break;
        }
    }

    private List<String> convertToListString(List<Recommend> recommends){
        List<String> inputUris = new ArrayList<>();
        for(Recommend node : recommends){
            inputUris.add(node.getUri());
        }
        return inputUris;
    }

    //======================TOP recommend=========================
    private void parserTopResponseData(){
        try {
            JSONArray jsonArray = new JSONArray(mResponse);
            for(int i = 0; i < jsonArray.length(); i++){
                JSONArray results = jsonArray.getJSONObject(i).getJSONArray("results");
                String uriType = results.getJSONObject(i).getString("uri");
                if(DbpediaConstant.isContext(uriType)) continue;
                for(int j = 0; j < results.length(); j++){
                    final float threshold = BigDecimal.valueOf(results.getJSONObject(i)
                            .getDouble("value")).floatValue();
                    final String label = results.getJSONObject(j).getString("label");
                    String abtract = results.getJSONObject(j).getString("abstract");
                    final String uri = results.getJSONObject(j).getString("uri");
                    final String image = results.getJSONObject(j).getString("image");
                    if(label.equals("null") || abtract.equals("null"))
                        continue;
                    Recommend recommend = new Recommend(label, uri, image, threshold);
                    insertTopRecommend(recommend);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void insertTopRecommend(Recommend recommend){
        mRecommendDatas.add(recommend);
        Collections.sort(mRecommendDatas, new Comparator<Recommend>() {
            @Override
            public int compare(Recommend recommend, Recommend t1) {
                return recommend.getThreshold() < t1.getThreshold()? 1 : -1;
            }
        });
        if(mRecommendDatas.size() > MAXTOPRESULT){
            mRecommendDatas.remove(mRecommendDatas.size() - 1);
        }
        mGridAdapter.notifyDataSetChanged();
    }

    //========================Normal recommend================

    private void parserNormalResponseData(){
        try {
            JSONArray jsonArray = new JSONArray(mResponse);
            for(int i = 0; i < jsonArray.length(); i++){
                String label = jsonArray.getJSONObject(i).getString("label");
                String uri = jsonArray.getJSONObject(i).getString("uri");
                String image = jsonArray.getJSONObject(i).getString("image");
                if(label.equals("null")){
                    continue;
                }
                Recommend recommend = new Recommend(label, uri, image);
                insertNormalRecommend(recommend);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void insertNormalRecommend(Recommend recommend){
        mRecommendDatas.add(recommend);
        mGridAdapter.notifyDataSetChanged();
    }

    @Override
    public void showDetails(Recommend recommend) {
        Intent intent = new Intent(getContext(), DetailsActivity.class);
        intent.putExtra(DetailsActivity.DATA, recommend.getUri());
        startActivity(intent);
    }

    @Override
    public void addSearch(Recommend recommend) {
        if(!isContain(recommend)){
            if(mSelectedRecommends.size() < MAXITEM){
                MessageObserverManager.getInstance().notifyAllObserver(recommend);
            }else{
                showLogAndToast("Max item. Can't choice!");
            }
        }else {
            showLogAndToast("Item added. Please choice other item!");
        }
    }

    private boolean isContain(Recommend recommend){
        for(Recommend rm : mSelectedRecommends){
            if(rm.getUri().equals(recommend.getUri())){
                return true;
            }
        }
        return false;
    }

    @Override
    public void exSearch(Recommend recommend) {
        List<String> recommends = new ArrayList<>();
        recommends.add(recommend.getUri());
        ((RecommendActivity)getActivity()).EXSearch(recommends);
    }

    private void showLog(String message){
        if(message != null){
            Log.d(TAG, message);
        }
    }

    private void showLogAndToast(final String message){
        showLog(message);
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
}