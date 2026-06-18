package com.vortexstore.services;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.vortexstore.models.Product;
import com.vortexstore.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private static FirebaseService instance;

    private FirebaseService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    public FirebaseAuth getAuth() { return auth; }
    public FirebaseFirestore getDb() { return db; }

    public void saveUser(User user, Runnable onSuccess, Consumer<String> onError) {
        db.collection("users")
            .document(user.getUid())
            .set(user)
            .addOnSuccessListener(aVoid -> onSuccess.run())
            .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    public void getUser(String uid, Consumer<User> onSuccess, Consumer<String> onError) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    onSuccess.accept(user);
                } else {
                    onError.accept("User not found");
                }
            })
            .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    public void getProducts(Consumer<List<Product>> onSuccess, Consumer<String> onError) {
        db.collection("products")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Product> products = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Product product = doc.toObject(Product.class);
                    if (product != null) {
                        product.setId(doc.getId());
                        products.add(product);
                    }
                }
                onSuccess.accept(products);
            })
            .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signOut() {
        auth.signOut();
    }
}
