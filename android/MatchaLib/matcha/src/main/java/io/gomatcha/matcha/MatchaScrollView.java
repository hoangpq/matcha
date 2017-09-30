package io.gomatcha.matcha;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.google.protobuf.InvalidProtocolBufferException;

import io.gomatcha.bridge.GoValue;
import io.gomatcha.matcha.proto.layout.PbLayout;
import io.gomatcha.matcha.proto.view.PbScrollView;

class MatchaScrollView extends MatchaChildView {
    ScrollView scrollView;
    MatchaLayout childView;
    MatchaViewNode viewNode;
    boolean hasOnTouchListener;
    int matchaX;
    int matchaY;

    static {
        MatchaView.registerView("gomatcha.io/matcha/view/scrollview", new MatchaView.ViewFactory() {
            @Override
            public MatchaChildView createView(Context context, MatchaViewNode node) {
                return new MatchaScrollView(context, node);
            }
        });
    }

    public MatchaScrollView(final Context context, MatchaViewNode node) {
        super(context);
        viewNode = node;
        this.setClipChildren(true);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                float ratio = (float)context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
                float scrollY = scrollView.getScrollY() / ratio; // For ScrollView
                float scrollX = scrollView.getScrollX() / ratio; // For HorizontalScrollView
                if (viewNode.rootView.updating || (scrollView.getScrollX() == matchaX && scrollView.getScrollY() == matchaY)) {
                    return;
                }
                matchaX = scrollView.getScrollX();
                matchaY = scrollView.getScrollY();

                PbLayout.Point point = PbLayout.Point.newBuilder().setX(scrollX).setY(scrollY).build();
                PbScrollView.ScrollEvent event = PbScrollView.ScrollEvent.newBuilder().setContentOffset(point).build();
                MatchaScrollView.this.viewNode.call("OnScroll", new GoValue(event.toByteArray()));
            }
        });
        addView(scrollView);

        childView = new MatchaLayout(context);
        scrollView.addView(childView);
    }

    @Override
    public void setNativeState(byte[] nativeState) {
        super.setNativeState(nativeState);
        try {
            PbScrollView.ScrollView proto = PbScrollView.ScrollView.parseFrom(nativeState);
            if (scrollView.isVerticalScrollBarEnabled() != proto.getShowsVerticalScrollIndicator()) {
                scrollView.setVerticalScrollBarEnabled(proto.getShowsVerticalScrollIndicator());
            }
            if (scrollView.isHorizontalScrollBarEnabled() != proto.getShowsHorizontalScrollIndicator()) {
                scrollView.setHorizontalScrollBarEnabled(proto.getShowsHorizontalScrollIndicator());
            }
            if (hasOnTouchListener != proto.getScrollEnabled()) {
                hasOnTouchListener = proto.getScrollEnabled();
                if (proto.getScrollEnabled()) {
                    scrollView.setOnTouchListener(null);
                } else {
                    scrollView.setOnTouchListener(new OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            return false;
                        }
                    });
                }
            }
        } catch (InvalidProtocolBufferException e) {
        }
    }
}
