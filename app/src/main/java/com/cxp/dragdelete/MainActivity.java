package com.cxp.dragdelete;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rv;
    private AD adapter;
    private ArrayList<String> list = new ArrayList<>();
    private TextView view;
    private ItemTouchHelper helper;
    private DragListener dragListener;
    private boolean up;
    private boolean needScaleBig = true;
    private boolean needScaleSmall = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.cxp.dragdelete.R.layout.activity_main);
        list.add("11111");
        list.add("22222");
        list.add("33333");
        list.add("44444");
        list.add("55555");
        list.add("66666");
        list.add("77777");
        list.add("88888");
//        list.add("99999");
        rv = (RecyclerView) findViewById(com.cxp.dragdelete.R.id.rv);
        view = findViewById(com.cxp.dragdelete.R.id.view);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
                outRect.set(0, 0, 5, 5);
            }

        });
        {
            //添加条目点击回调
            rv.addOnItemTouchListener(new com.cxp.dragdelete.OnRecyclerItemClickListener(rv) {

                @Override
                public void onItemClick(RecyclerView.ViewHolder vh) {
                    int adapterPosition = vh.getAdapterPosition();
                    Toast.makeText(MainActivity.this, list.get(adapterPosition), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onItemLongClick(RecyclerView.ViewHolder vh) {
                    //如果item不是最后一个，则执行拖拽
                    needScaleBig = true;
                    needScaleSmall = true;
                    if (list.size() != 9) {
                        helper.startDrag(vh);
                        return;
                    }
                    if (vh.getLayoutPosition() != list.size() - 1) {
                        helper.startDrag(vh);
                    }
                }
            });
        }
        {
            //设置拖拽回调，用于底部提示view显示隐藏
            dragListener = new DragListener() {
                @Override
                public void deleteState(boolean delete) {
                    if (delete) {
                        view.setBackgroundResource(com.cxp.dragdelete.R.color.holo_red_dark);
                        view.setText(getResources().getString(com.cxp.dragdelete.R.string.post_delete_tv_s));
                    } else {
                        view.setText(getResources().getString(com.cxp.dragdelete.R.string.post_delete_tv_d));
                        view.setBackgroundResource(com.cxp.dragdelete.R.color.holo_red_light);
                    }
                }

                @Override
                public void dragState(boolean start) {
                    if (start) {
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.GONE);
                    }
                }
            };
        }
        {
            //recyclerview条目拖拽管理器
            helper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
                @Override
                public boolean isLongPressDragEnabled() {
                    return false;
                }

                @Override
                public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    return makeMovementFlags(ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0);
                }

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder targetVh) {
                    int fromPosition = viewHolder.getAdapterPosition();//得到item原来的position
                    int toPosition = targetVh.getAdapterPosition();//得到目标position
                    if (fromPosition < toPosition) {
                        for (int i = fromPosition; i < toPosition; i++) {
                            Collections.swap(list, i, i + 1);
                        }
                    } else {
                        for (int i = fromPosition; i > toPosition; i--) {
                            Collections.swap(list, i, i - 1);
                        }
                    }
                    adapter.notifyItemMoved(fromPosition, toPosition);
                    return true;
                }

                @Override
                public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    adapter.notifyDataSetChanged();
                    resetState();
                }

                //自定义拖动与滑动交互
                @Override
                public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                    if (null == dragListener) {
                        return;
                    }
                    dragListener.dragState(true);//显示删除区域
                    if (needScaleBig) {//如果需要执行放大动画
                        viewHolder.itemView.animate().scaleXBy(0.2f).scaleYBy(0.2f).setDuration(100);
                        needScaleBig = false;//执行完成放大动画,标记改掉
                        needScaleSmall = false;//默认不需要执行缩小动画，当执行完成放大 并且松手后才允许执行
                    }
                    if (dY >= (recyclerView.getHeight()
                            - viewHolder.itemView.getBottom()//item底部距离recyclerView顶部高度
                    )) {//拖到删除处
                        dragListener.deleteState(true);
                        if (up) {//在删除处放手，则删除item
                            viewHolder.itemView.setVisibility(View.INVISIBLE);//先设置不可见，如果不设置的话，会看到viewHolder返回到原位置时才消失，因为remove会在viewHolder动画执行完成后才将viewHolder删除
                            list.remove(viewHolder.getAdapterPosition());
                            adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                            resetState();
                            return;
                        }
                    } else {//没有到删除处
                        if (View.INVISIBLE == viewHolder.itemView.getVisibility()) {//如果viewHolder不可见，则表示用户放手，重置删除区域状态
                            dragListener.dragState(false);
                        }
                        if (needScaleSmall) {//需要松手后才能执行
                            viewHolder.itemView.animate().scaleXBy(1f).scaleYBy(1f).setDuration(100);
                        }
                        dragListener.deleteState(false);
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }

                @Override
                public long getAnimationDuration(RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
                    //手指放开
                    needScaleSmall = true;
                    up = true;
                    return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
                }

                //当长按选中item的时候（拖拽开始的时候）调用
                @Override
                public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                    if (ItemTouchHelper.ACTION_STATE_DRAG == actionState && dragListener != null) {
                        dragListener.dragState(true);
                    }
                    super.onSelectedChanged(viewHolder, actionState);
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                }
            });
        }
        adapter = new AD();
        rv.setAdapter(adapter);
        helper.attachToRecyclerView(rv);
    }

    /**
     * 重置
     */
    private void resetState() {
        if (dragListener != null) {
            dragListener.deleteState(false);
            dragListener.dragState(false);
        }
        up = false;
    }

    interface DragListener {
        /**
         * 用户是否将 item拖动到删除处，根据状态改变颜色
         *
         * @param delete
         */
        void deleteState(boolean delete);

        /**
         * 是否于拖拽状态
         *
         * @param start
         */
        void dragState(boolean start);
    }

    /**
     * 像素单位转换 dp到px
     */
    public int dpTopx(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    class AD extends RecyclerView.Adapter<VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(View.inflate(MainActivity.this, com.cxp.dragdelete.R.layout.item_rv, null));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.textView.setText(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    class VH extends RecyclerView.ViewHolder {

        private final TextView textView;

        public VH(View itemView) {
            super(itemView);
            textView = itemView.findViewById(com.cxp.dragdelete.R.id.tv);
        }
    }
}
