package net.fishandwhistle.openpos.items;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Created by dewey on 2016-10-13.
 */

public class MaxSizeListView extends ListView {


    public MaxSizeListView(Context context) {
        super(context);
    }

    public MaxSizeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaxSizeListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setMeasuredDimension(getMeasuredWidth(), Math.min(200, getMeasuredHeight()));
    }
}
