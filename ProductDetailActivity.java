package com.vortexstore.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vortexstore.R;
import com.vortexstore.models.Product;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {
    
    private ImageView ivProductImage, ivBack;
    private TextView tvProductName, tvProductPrice, tvProductCategory, 
                      tvProductBadge, tvProductDescription, tvStock;
    private EditText etQuantity;
    private Button btnBuyNow, btnAddToCart;
    
    private Product product;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Get product data from intent
        if (getIntent().hasExtra("product")) {
            product = (Product) getIntent().getSerializableExtra("product");
        }
        
        initViews();
        setupListeners();
        displayProductDetails();
    }
    
    private void initViews() {
        ivProductImage = findViewById(R.id.ivProductImage);
        ivBack = findViewById(R.id.ivBack);
        tvProductName = findViewById(R.id.tvProductName);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvProductCategory = findViewById(R.id.tvProductCategory);
        tvProductBadge = findViewById(R.id.tvProductBadge);
        tvProductDescription = findViewById(R.id.tvProductDescription);
        tvStock = findViewById(R.id.tvStock);
        etQuantity = findViewById(R.id.etQuantity);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnAddToCart = findViewById(R.id.btnAddToCart);
    }
    
    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        btnBuyNow.setOnClickListener(v -> processPurchase());
        
        btnAddToCart.setOnClickListener(v -> addToCart());
    }
    
    private void displayProductDetails() {
        if (product == null) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Load image
        Glide.with(this)
                .load(product.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(ivProductImage);
        
        // Set text
        tvProductName.setText(product.getName());
        
        // Format price
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String price = formatter.format(product.getPrice());
        tvProductPrice.setText(price);
        
        // Category
        if (product.getCategory() != null) {
            tvProductCategory.setText(product.getCategory().toUpperCase());
            tvProductCategory.setVisibility(View.VISIBLE);
        } else {
            tvProductCategory.setVisibility(View.GONE);
        }
        
        // Badge
        if (product.getBadge() != null && !product.getBadge().isEmpty()) {
            tvProductBadge.setText(product.getBadge());
            tvProductBadge.setVisibility(View.VISIBLE);
        } else {
            tvProductBadge.setVisibility(View.GONE);
        }
        
        // Description
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            tvProductDescription.setText(product.getDescription());
            tvProductDescription.setVisibility(View.VISIBLE);
        } else {
            tvProductDescription.setText("Tidak ada deskripsi produk ini.");
        }
        
        // Stock (if available)
        tvStock.setVisibility(View.GONE);
    }
    
    private void processPurchase() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String quantityStr = etQuantity.getText().toString().trim();
        if (quantityStr.isEmpty() || Integer.parseInt(quantityStr) <= 0) {
            Toast.makeText(this, "Masukkan jumlah yang valid", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int quantity = Integer.parseInt(quantityStr);
        double totalPrice = product.getPrice() * quantity;
        
        // Create order data
        Map<String, Object> order = new HashMap<>();
        order.put("productId", product.getId());
        order.put("productName", product.getName());
        order.put("quantity", quantity);
        order.put("totalPrice", totalPrice);
        order.put("userId", auth.getCurrentUser().getUid());
        order.put("userEmail", auth.getCurrentUser().getEmail());
        order.put("status", "pending");
        order.put("createdAt", System.currentTimeMillis());
        
        // Save to Firestore
        db.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Pesanan berhasil dibuat!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal membuat pesanan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void addToCart() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String quantityStr = etQuantity.getText().toString().trim();
        if (quantityStr.isEmpty() || Integer.parseInt(quantityStr) <= 0) {
            Toast.makeText(this, "Masukkan jumlah yang valid", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int quantity = Integer.parseInt(quantityStr);
        
        // Create cart item
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", product.getId());
        cartItem.put("productName", product.getName());
        cartItem.put("productImage", product.getImageUrl());
        cartItem.put("price", product.getPrice());
        cartItem.put("quantity", quantity);
        cartItem.put("userId", auth.getCurrentUser().getUid());
        cartItem.put("addedAt", System.currentTimeMillis());
        
        // Save to Firestore
        db.collection("carts")
                .add(cartItem)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Ditambahkan ke keranjang!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menambahkan ke keranjang", Toast.LENGTH_SHORT).show();
                });
    }
}
