package com.vortexstore.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;
import com.vortexstore.R;
import com.vortexstore.models.Product;
import com.vortexstore.services.FirebaseService;
import com.vortexstore.ui.adapters.ProductAdapter;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvProducts, rvVouchers, rvPulsa;
    private TextView tvUserName, tvUserEmail;
    private ImageView ivUserPhoto, ivLogout;
    
    private FirebaseService firebaseService;
    private ProductAdapter productAdapter, voucherAdapter, pulsaAdapter;
    private List<Product> products = new ArrayList<>();
    private List<Product> vouchers = new ArrayList<>();
    private List<Product> pulsaList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        firebaseService = FirebaseService.getInstance();
        
        initViews();
        setupUserInfo();
        setupRecyclerViews();
        setupListeners();
        loadProducts();
    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvProducts = findViewById(R.id.rvProducts);
        rvVouchers = findViewById(R.id.rvVouchers);
        rvPulsa = findViewById(R.id.rvPulsa);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        ivUserPhoto = findViewById(R.id.ivUserPhoto);
        ivLogout = findViewById(R.id.ivLogout);
    }

    private void setupUserInfo() {
        FirebaseUser user = firebaseService.getCurrentUser();
        if (user != null) {
            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(ivUserPhoto);
            }
        }
    }

    private void setupRecyclerViews() {
        // Products RecyclerView (Horizontal)
        productAdapter = new ProductAdapter(this, products, "game");
        rvProducts.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        rvProducts.setAdapter(productAdapter);
        
        // Vouchers RecyclerView (Horizontal)
        voucherAdapter = new ProductAdapter(this, vouchers, "voucher");
        rvVouchers.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        rvVouchers.setAdapter(voucherAdapter);
        
        // Pulsa RecyclerView (Horizontal)
        pulsaAdapter = new ProductAdapter(this, pulsaList, "pulsa");
        rvPulsa.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        rvPulsa.setAdapter(pulsaAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadProducts();
            swipeRefresh.setRefreshing(false);
        });
        
        ivLogout.setOnClickListener(v -> logout());
    }

    private void loadProducts() {
        firebaseService.getProducts(
                productsList -> {
                    this.products.clear();
                    this.vouchers.clear();
                    this.pulsaList.clear();
                    
                    for (Product product : productsList) {
                        if ("game".equals(product.getCategory())) {
                            this.products.add(product);
                        } else if ("voucher".equals(product.getCategory())) {
                            this.vouchers.add(product);
                        } else if ("pulsa".equals(product.getCategory())) {
                            this.pulsaList.add(product);
                        }
                    }
                    
                    productAdapter.notifyDataSetChanged();
                    voucherAdapter.notifyDataSetChanged();
                    pulsaAdapter.notifyDataSetChanged();
                },
                error -> Toast.makeText(this, "Error loading products: " + error, Toast.LENGTH_SHORT).show()
        );
    }

    private void logout() {
        firebaseService.signOut();
        finish();
    }
}
