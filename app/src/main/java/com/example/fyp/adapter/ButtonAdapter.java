package com.example.fyp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fyp.R;
import java.util.List;

public class ButtonAdapter extends RecyclerView.Adapter<ButtonAdapter.ButtonViewHolder> {
    private Context context;
    private List<String> buttonLabels;

    public ButtonAdapter(Context context, List<String> buttonLabels) {
        this.context = context;
        this.buttonLabels = buttonLabels;
    }

    @NonNull
    @Override
    public ButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_button, parent, false);
        return new ButtonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ButtonViewHolder holder, int position) {
        String label = buttonLabels.get(position);
        holder.button.setText(label);
    }

    @Override
    public int getItemCount() {
        return buttonLabels.size();
    }

    public static class ButtonViewHolder extends RecyclerView.ViewHolder {
        Button button;

        public ButtonViewHolder(View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.button);
        }
    }
}
