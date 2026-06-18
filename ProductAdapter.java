package com.vortexstore.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vortexstore.R;
import com.vortexstore.models.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private Context context;
    private List<Product> products;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        
        holder.tvName.setText(product.getName());
        holder.tvPrice.setText("Rp " + String.format("%,.0f", product.getPrice()));
        
        if (product.getBadge() != null && !product.getBadge().isEmpty()) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(product.getBadge());
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }
        
        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.ivImage);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice, tvBadge;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivProductImage);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }
}
