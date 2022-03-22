package com.derrick.wellnesscheck.controller;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;

public class SwipeToContactCallback extends ItemTouchHelper.SimpleCallback{
    private final ResourcesRecyclerAdapter adapter;
    private final Drawable[] icons;
    private final ColorDrawable[] backgrounds = new ColorDrawable[]{
            new ColorDrawable(Color.GREEN),
            new ColorDrawable(Color.BLUE)
    };

    public SwipeToContactCallback(ResourcesRecyclerAdapter adapter){
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        icons =  new Drawable[]{
                ContextCompat.getDrawable(adapter.getContext(), android.R.drawable.ic_menu_call),
                ContextCompat.getDrawable(adapter.getContext(), R.drawable.ic_sms)
        };
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getBindingAdapterPosition();
        adapter.contact(pos);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        int i = ((ResourcesRecyclerAdapter.ViewHolder) viewHolder).sms ? 1 : 0;
        int backgroundCornerOffset = 20;
        int iconMargin = (itemView.getHeight() - icons[i].getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - icons[i].getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + icons[i].getIntrinsicHeight();

        if (dX > 0) { // Swiping to the right
            int iconRight = itemView.getLeft() + iconMargin + icons[i].getIntrinsicWidth();
            int iconLeft = itemView.getLeft() + iconMargin;
            icons[i].setBounds(iconLeft, iconTop, iconRight, iconBottom);

            backgrounds[i].setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + ((int) dX) + backgroundCornerOffset,
                    itemView.getBottom());
        } else if (dX < 0) { // Swiping to the left
            int iconLeft = itemView.getRight() - iconMargin - icons[i].getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            icons[i].setBounds(iconLeft, iconTop, iconRight, iconBottom);

            backgrounds[i].setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                    itemView.getTop(), itemView.getRight(), itemView.getBottom());
        } else { // view is unSwiped
            backgrounds[i].setBounds(0, 0, 0, 0);
            icons[i].setBounds(0, 0, 0, 0);
        }

        backgrounds[i].draw(c);
        icons[i].draw(c);
    }
}
