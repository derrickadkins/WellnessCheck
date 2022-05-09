package com.derrick.wellnesscheck.ui.fragments.contacts;

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

public class SwipeOnContactCallback extends ItemTouchHelper.SimpleCallback {

    private final ContactsRecyclerAdapter adapter;
    private final Drawable[] icons;
    private final ColorDrawable[] backgrounds = new ColorDrawable[]{
            new ColorDrawable(Color.BLUE),
            new ColorDrawable(Color.GREEN)
    };
    private final int transparent, white;

    public SwipeOnContactCallback(ContactsRecyclerAdapter adapter){
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        icons = new Drawable[]{
                ContextCompat.getDrawable(adapter.getContext(), R.drawable.ic_sms),
                ContextCompat.getDrawable(adapter.getContext(), android.R.drawable.ic_menu_call)
        };
        transparent = adapter.getContext().getColor(android.R.color.transparent);
        white = adapter.getContext().getColor(android.R.color.white);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getBindingAdapterPosition();
        adapter.contact(pos, direction != ItemTouchHelper.LEFT);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        int backgroundCornerOffset = 20;
        int i = 0;

        viewHolder.itemView.setBackgroundColor(dX == 0 ? transparent : white);

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
            i = 1;
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