package xyz.sunting.xrecyclerview;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import demo.xrecyclerview.sunting.xyz.xrecyclerview.R;

public class MainActivity extends Activity implements XRecyclerView.OnRefreshListener,
        XRecyclerView.OnLoadMoreListener {

    XRecyclerView mRecyclerView;
    FaceAdapter mAdapter;
    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (XRecyclerView) findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new FaceAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setPullRefreshEnable(true);
        mRecyclerView.setPullLoadEnable(true);
        mRecyclerView.setOnRefreshListener(this);
        mRecyclerView.setOnLoadMoreListener(this);

        mHandler = new Handler();
    }

    @Override
    public void onRefresh() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.setCount(20);
                mAdapter.notifyDataSetChanged();
                onLoad();
            }
        }, 3000);
    }

    @Override
    public void onLoadMore() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.setCount(mAdapter.mCount + 10);
                mAdapter.notifyDataSetChanged();
                onLoad();
            }
        }, 3000);
    }

    private void onLoad() {
        mRecyclerView.stopRefresh();
        mRecyclerView.stopLoadMore();
        mRecyclerView.setRefreshTime("刚刚");
    }

    private static class FaceViewHolder extends RecyclerView.ViewHolder {

        FaceViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class FaceAdapter extends RecyclerView.Adapter<FaceViewHolder> {
        private int mCount = 20;

        public FaceAdapter() {
            setHasStableIds(true);
        }

        public void setCount(int count) {
            mCount = count;
        }

        @Override
        public FaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FaceViewHolder(LayoutInflater.from(getApplicationContext())
                    .inflate(R.layout.item, parent, false));
        }

        @Override
        public void onBindViewHolder(final FaceViewHolder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return mCount;
        }
    }
}
