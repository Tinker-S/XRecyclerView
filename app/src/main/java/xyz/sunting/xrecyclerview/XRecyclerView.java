package xyz.sunting.xrecyclerview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import demo.xrecyclerview.sunting.xyz.xrecyclerview.R;

public class XRecyclerView extends RecyclerView {
    private static final int TYPE_REFRESH_HEADER = 10000;
    private static final int TYPE_FOOTER = 10001;

    private float mLastY = -1;
    private Scroller mScroller;

    private OnRefreshListener mRefreshListener;
    private OnLoadMoreListener mLoadMoreListener;

    private XRecyclerViewHeader mHeaderView;
    private RelativeLayout mHeaderViewContent;
    private TextView mHeaderTimeView;
    private int mHeaderViewHeight;
    private boolean mEnablePullRefresh = true;
    private boolean mPullRefreshing = false;

    private XRecyclerViewFooter mFooterView;
    private boolean mEnablePullLoad;
    private boolean mPullLoading;

    private int mScrollBack;
    private final static int SCROLLBACK_HEADER = 0;
    private final static int SCROLLBACK_FOOTER = 1;

    private final static int SCROLL_DURATION = 400;
    private final static int PULL_LOAD_MORE_DELTA = 50;
    private final static float OFFSET_RADIO = 1.8f;

    public XRecyclerView(Context context) {
        super(context);
        initWithContext(context);
    }

    public XRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initWithContext(context);
    }

    private void initWithContext(Context context) {
        mScroller = new Scroller(context, new DecelerateInterpolator());

        mHeaderView = new XRecyclerViewHeader(context);
        mHeaderView.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        mHeaderViewContent = (RelativeLayout) mHeaderView
                .findViewById(R.id.content);
        mHeaderTimeView = (TextView) mHeaderView
                .findViewById(R.id.time);

        mFooterView = new XRecyclerViewFooter(context);
        mFooterView.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mHeaderViewHeight = mHeaderViewContent.getHeight();
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }

    public void setPullRefreshEnable(boolean enable) {
        mEnablePullRefresh = enable;
        if (!mEnablePullRefresh) {
            mHeaderViewContent.setVisibility(View.INVISIBLE);
        } else {
            mHeaderViewContent.setVisibility(View.VISIBLE);
        }
    }

    public void setPullLoadEnable(boolean enable) {
        mEnablePullLoad = enable;
        if (!mEnablePullLoad) {
            mFooterView.hide();
            mFooterView.setOnClickListener(null);
        } else {
            mPullLoading = false;
            mFooterView.show();
            mFooterView.setState(XRecyclerViewFooter.STATE_NORMAL);
            mFooterView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startLoadMore();
                }
            });
        }
    }

    public void stopRefresh() {
        if (mPullRefreshing) {
            mPullRefreshing = false;
            resetHeaderHeight();
        }
    }

    public void stopLoadMore() {
        if (mPullLoading) {
            mPullLoading = false;
            mFooterView.setState(XRecyclerViewFooter.STATE_NORMAL);
        }
    }

    public void setRefreshTime(String time) {
        mHeaderTimeView.setText(time);
    }

    private void updateHeaderHeight(float delta) {
        mHeaderView.setVisiableHeight((int) delta
                + mHeaderView.getVisiableHeight());
        if (mEnablePullRefresh && !mPullRefreshing) {
            if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
                mHeaderView.setState(XRecyclerViewHeader.STATE_READY);
            } else {
                mHeaderView.setState(XRecyclerViewHeader.STATE_NORMAL);
            }
        }
        scrollToPosition(0);
    }

    private void resetHeaderHeight() {
        int height = mHeaderView.getVisiableHeight();
        if (height == 0) {
            return;
        }
        if (mPullRefreshing && height <= mHeaderViewHeight) {
            return;
        }
        int finalHeight = 0;
        if (mPullRefreshing && height > mHeaderViewHeight) {
            finalHeight = mHeaderViewHeight;
        }
        mScrollBack = SCROLLBACK_HEADER;
        mScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);
        invalidate();
    }

    private void updateFooterHeight(float delta) {
        int height = mFooterView.getBottomMargin() + (int) delta;
        if (mEnablePullLoad && !mPullLoading) {
            if (height > PULL_LOAD_MORE_DELTA) {
                mFooterView.setState(XRecyclerViewFooter.STATE_READY);
            } else {
                mFooterView.setState(XRecyclerViewFooter.STATE_NORMAL);
            }
        }
        mFooterView.setBottomMargin(height);
        scrollToPosition(mWrapAdapter.getItemCount() - 1);
    }

    private void resetFooterHeight() {
        int bottomMargin = mFooterView.getBottomMargin();
        if (bottomMargin > 0) {
            mScrollBack = SCROLLBACK_FOOTER;
            mScroller.startScroll(0, bottomMargin, 0, -bottomMargin,
                    SCROLL_DURATION);
            invalidate();
        }
    }

    private void startLoadMore() {
        mPullLoading = true;
        mFooterView.setState(XRecyclerViewFooter.STATE_LOADING);
        if (mLoadMoreListener != null) {
            mLoadMoreListener.onLoadMore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mLastY == -1) {
            mLastY = ev.getRawY();
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float deltaY = ev.getRawY() - mLastY;
                mLastY = ev.getRawY();
                if (isOnTop() && (mHeaderView.getVisiableHeight() > 0 || deltaY > 0)) {
                    updateHeaderHeight(deltaY / OFFSET_RADIO);
                } else if (getLastVisiblePosition() == mWrapAdapter.getItemCount() - 1
                        && (mFooterView.getBottomMargin() > 0 || deltaY < 0)) {
                    updateFooterHeight(-deltaY / OFFSET_RADIO);
                }
                break;
            default:
                mLastY = -1;
                if (isOnTop()) {
                    if (mEnablePullRefresh
                            && mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
                        mPullRefreshing = true;
                        mHeaderView.setState(XRecyclerViewHeader.STATE_REFRESHING);
                        if (mRefreshListener != null) {
                            mRefreshListener.onRefresh();
                        }
                    }
                    resetHeaderHeight();
                } else if (getLastVisiblePosition() == mWrapAdapter.getItemCount() - 1) {
                    if (mEnablePullLoad
                            && mFooterView.getBottomMargin() > PULL_LOAD_MORE_DELTA
                            && !mPullLoading) {
                        startLoadMore();
                    }
                    resetFooterHeight();
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private int getFirstVisiblePosition() {
        LayoutManager layoutManager = getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            throw new IllegalStateException("Only support LinearLayoutManager!");
        }
        return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
    }

    private int getLastVisiblePosition() {
        LayoutManager layoutManager = getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            throw new IllegalStateException("Only support LinearLayoutManager!");
        }
        return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScrollBack == SCROLLBACK_HEADER) {
                mHeaderView.setVisiableHeight(mScroller.getCurrY());
            } else {
                mFooterView.setBottomMargin(mScroller.getCurrY());
            }
            postInvalidate();
        }
        super.computeScroll();
    }

    public void setOnRefreshListener(OnRefreshListener l) {
        mRefreshListener = l;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener l) {
        mLoadMoreListener = l;
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    private WrapAdapter mWrapAdapter;

    @Override
    public void setAdapter(Adapter adapter) {
        mWrapAdapter = new WrapAdapter(adapter);
        super.setAdapter(mWrapAdapter);
    }

    @Override
    public Adapter getAdapter() {
        if (mWrapAdapter != null) {
            return mWrapAdapter.adapter;
        }
        return null;
    }

    public boolean isOnTop() {
        if (getChildCount() == 0) {
            return true;
        }
        return getPaddingTop() + getTop() == getChildAt(0).getTop();
    }

    public boolean isOnBottom() {
        if (getChildCount() == 0) {
            return true;
        }
        return getBottom() - getPaddingBottom() == getChildAt(getChildCount() - 1).getBottom();
    }


    @SuppressWarnings("unchecked")
    public class WrapAdapter extends RecyclerView.Adapter<ViewHolder> {

        private RecyclerView.Adapter adapter;

        public WrapAdapter(RecyclerView.Adapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_REFRESH_HEADER) {
                return new SimpleViewHolder(mHeaderView);
            } else if (viewType == TYPE_FOOTER) {
                return new SimpleViewHolder(mFooterView);
            }
            return adapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            int type = getItemViewType(position);
            if (type == TYPE_REFRESH_HEADER || type == TYPE_FOOTER) {
                return;
            }
            adapter.onBindViewHolder(holder, position - 1);
        }

        @Override
        public int getItemCount() {
            int count = adapter.getItemCount();
            if (mEnablePullRefresh) {
                count++;
            }
            if (mEnablePullLoad) {
                count++;
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            if (mEnablePullRefresh && position == 0) {
                return TYPE_REFRESH_HEADER;
            } else if (mEnablePullLoad && position == getItemCount() - 1) {
                return TYPE_FOOTER;
            }
            return 0;
        }

        @Override
        public long getItemId(int position) {
            int type = getItemViewType(position);
            if (type == TYPE_REFRESH_HEADER || type == TYPE_FOOTER) {
                return -1;
            }
            return adapter.getItemId(position);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            adapter.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            adapter.onDetachedFromRecyclerView(recyclerView);
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            adapter.onViewAttachedToWindow(holder);
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            adapter.onViewDetachedFromWindow(holder);
        }

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            adapter.onViewRecycled(holder);
        }

        @Override
        public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
            return adapter.onFailedToRecycleView(holder);
        }

        @Override
        public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
            adapter.unregisterAdapterDataObserver(observer);
        }

        @Override
        public void registerAdapterDataObserver(AdapterDataObserver observer) {
            adapter.registerAdapterDataObserver(observer);
        }

        private class SimpleViewHolder extends RecyclerView.ViewHolder {
            SimpleViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
