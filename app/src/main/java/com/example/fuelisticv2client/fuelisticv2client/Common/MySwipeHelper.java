package com.example.fuelisticv2client.fuelisticv2client.Common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class MySwipeHelper extends ItemTouchHelper.SimpleCallback {

    public static int BUTTON_WIDTH ;
    private RecyclerView recyclerView;
    private List<UnderlayButton> buttons;
    private GestureDetector gestureDetector;
    private int swipedPos = -1;
    private float swipeThreshold = 0.5f;
    private Map<Integer, List<UnderlayButton>> buttonsBuffer;
    private Queue<Integer> recoverQueue;


    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            for (UnderlayButton button : buttons){
                if(button.onClick(e.getX(), e.getY()))
                    break;
            }

            return true;
        }
    };

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent e) {
            if (swipedPos < 0) return false;

            try {
                Point point = new Point((int) e.getRawX(), (int) e.getRawY());

                RecyclerView.ViewHolder swipedViewHolder = recyclerView.findViewHolderForAdapterPosition(swipedPos);
                View swipedItem = swipedViewHolder.itemView;
                Rect rect = new Rect();
                swipedItem.getGlobalVisibleRect(rect);

                if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_UP ||e.getAction() == MotionEvent.ACTION_MOVE) {
                    if (rect.top < point.y && rect.bottom > point.y)
                        gestureDetector.onTouchEvent(e);
                    else {
                        recoverQueue.add(swipedPos);
                        swipedPos = -1;
                        recoverSwipedItem();
                    }
                }
                return false;
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    public MySwipeHelper(Context context, RecyclerView recyclerView, int BUTTON_WIDTH) {
        super(0, ItemTouchHelper.LEFT);
        this.recyclerView = recyclerView;
        this.buttons = new ArrayList<>();
        this.gestureDetector = new GestureDetector(context, gestureListener);
        this.recyclerView.setOnTouchListener(onTouchListener);
        MySwipeHelper.BUTTON_WIDTH =BUTTON_WIDTH;
        buttonsBuffer = new HashMap<>();
        recoverQueue = new LinkedList<Integer>(){
            @Override
            public boolean add(Integer o) {
                if (contains(o))
                    return false;
                else
                    return super.add(o);
            }
        };

        attachSwipe();
    }


    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();

        if (swipedPos != pos)
            recoverQueue.add(swipedPos);

        swipedPos = pos;

        if (buttonsBuffer.containsKey(swipedPos))
            buttons = buttonsBuffer.get(swipedPos);
        else
            buttons.clear();

        buttonsBuffer.clear();
        swipeThreshold = 0.5f * buttons.size() * BUTTON_WIDTH;
        recoverSwipedItem();
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return swipeThreshold;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return 0.1f * defaultValue;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return 5.0f * defaultValue;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        int pos = viewHolder.getAdapterPosition();
        float translationX = dX;
        View itemView = viewHolder.itemView;

        if (pos < 0){
            swipedPos = pos;
            return;
        }

        if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
            if(dX < 0) {
                List<UnderlayButton> buffer = new ArrayList<>();

                if (!buttonsBuffer.containsKey(pos)){
                    instantiateUnderlayButton(viewHolder, buffer);
                    buttonsBuffer.put(pos, buffer);
                }
                else {
                    buffer = buttonsBuffer.get(pos);
                }

                translationX = dX * buffer.size() * BUTTON_WIDTH / itemView.getWidth();
                drawButtons(c, itemView, buffer, pos, translationX);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive);
    }

    private synchronized void recoverSwipedItem(){
        while (!recoverQueue.isEmpty()){
            int pos = recoverQueue.poll();
            if (pos > -1) {
                recyclerView.getAdapter().notifyItemChanged(pos);
            }
        }
    }

    private void drawButtons(Canvas c, View itemView, List<UnderlayButton> buffer, int pos, float dX){
        float right = itemView.getRight();
        float dButtonWidth = (-1) * dX / buffer.size();

        for (UnderlayButton button : buffer) {
            float left = right - dButtonWidth;
            button.onDraw(
                    c,
                    new RectF(
                            left,
                            itemView.getTop(),
                            right,
                            itemView.getBottom()
                    ),
                    pos
            );

            right = left;
        }
    }

    public void attachSwipe(){
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public abstract void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons);

    public static class UnderlayButton {
        private String text;
        private int imageResId,textSize;
        private int color;
        private int pos;
        private RectF clickRegion;
        private Context context;
        private Resources resources;
        private UnderlayButtonClickListener clickListener;

        public UnderlayButton(Context context,String text, int textSize ,int imageResId, int color,  UnderlayButtonClickListener clickListener) {
            this.text = text;
            this.imageResId = imageResId;
            this.textSize = textSize;
            this.color = color;
            this.context = context;
            this.clickListener = clickListener;
            resources=context.getResources();
        }

//
//        public UnderlayButton(Context context, String text, int textSize, int imageResId, int ds) {
//        }

        //        public UnderlayButton(String text, int imageResId, int color, UnderlayButtonClickListener clickListener) {
//            this.text = text;
//            this.imageResId = imageResId;
//            this.color = color;
//            this.clickListener = clickListener;
//        }

        public boolean onClick(float x, float y){
            if (clickRegion != null && clickRegion.contains(x, y)){
                clickListener.onClick(pos);
                return true;
            }

            return false;
        }

        public void onDraw(Canvas c, RectF rect, int pos){
            Paint p = new Paint();

            // Draw background
            p.setColor(color);
            c.drawRect(rect, p);

            // Draw Text
            p.setColor(Color.WHITE);
            //    p.setTextSize(LayoutHelper.getPx(HomeFragment.getAppContext(), 12));  I T   W A S    B E F O  R E
            p.setTextSize(textSize);

            Rect r = new Rect();
            float cHeight = rect.height();
            float cWidth = rect.width();
            p.setTextAlign(Paint.Align.LEFT);
            p.getTextBounds(text, 0, text.length(), r);
//
//            I T  WAS BEFORE
//            float y = cHeight / 2f + r.height() / 2f - r.bottom;
//            float x = cWidth / 2f - r.width() / 2f - r.left;
//c.drawText(text, rect.left + x, rect.top + y, p);

//            C H A N G E S    F  R O M    H E R E

            float x = 0;
            float y = 0;

            if (imageResId==0)
            {
                x = cWidth / 2f - r.width() / 2f - r.left;
                y = cHeight / 2f + r.height() / 2f - r.bottom;
                c.drawText(text, rect.left + x, rect.top + y, p);
            }
            else {
                Drawable d = ContextCompat.getDrawable(context,imageResId);
                Bitmap bitmap= drawableToBitmap(d);

                c.drawBitmap(bitmap,(rect.left+rect.right)/(float) 2.2,(rect.top+rect.bottom)/(float) 2.2,p);
            }

            clickRegion = rect;
            this.pos = pos;
        }

        private Bitmap drawableToBitmap(Drawable d) {
            if (d instanceof BitmapDrawable)
            {
                return ((BitmapDrawable)d).getBitmap();
            }
            Bitmap bitmap= Bitmap.createBitmap(d.getIntrinsicWidth(),d.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
            Canvas canvas= new Canvas(bitmap);
            d.setBounds(0,0,canvas.getWidth(),canvas.getHeight());
            d.draw(canvas);
            return bitmap;
        }
    }

    public interface UnderlayButtonClickListener {
        void onClick(int pos);
    }
}
