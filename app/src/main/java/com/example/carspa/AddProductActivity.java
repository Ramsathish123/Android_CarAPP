package com.example.carspa;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class AddProductActivity extends AppCompatActivity {

    private AutoCompleteTextView itemName;
    private EditText quantity, rate;
    private Button addProductButton;
    private DatabaseReference productsRef;

    private final ArrayList<String> itemNames = new ArrayList<>();
    private final HashMap<String, Product> productMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        itemName = findViewById(R.id.itemName);
        quantity = findViewById(R.id.quantity);
        rate = findViewById(R.id.rate);
        addProductButton = findViewById(R.id.addProductButton);

        // Firebase reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        productsRef = database.getReference("products");

        // Fetch product names and data
        fetchProductItems();

        // When an item is selected, populate qty and rate
        itemName.setOnItemClickListener((adapterView, view, i, l) -> {
            String selectedName = adapterView.getItemAtPosition(i).toString();
            Product selectedProduct = productMap.get(selectedName);

            if (selectedProduct != null) {
                quantity.setText(String.valueOf(selectedProduct.quantity));
                rate.setText(String.valueOf(selectedProduct.rate));
            }
        });

        addProductButton.setOnClickListener(v -> addProduct());
    }

    private void fetchProductItems() {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                itemNames.clear();
                productMap.clear();

                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    Product product = productSnapshot.getValue(Product.class);
                    if (product != null && product.itemName != null) {
                        itemNames.add(product.itemName);
                        productMap.put(product.itemName, product);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddProductActivity.this,
                        android.R.layout.simple_dropdown_item_1line, itemNames);
                itemName.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AddProductActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addProduct() {
        String name = itemName.getText().toString().trim();
        String qtyStr = quantity.getText().toString().trim();
        String rateStr = rate.getText().toString().trim();

        if (name.isEmpty() || qtyStr.isEmpty() || rateStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        double rate = Double.parseDouble(rateStr);

        String key = productsRef.push().getKey();
        Product product = new Product(name, qty, rate);

        if (key != null) {
            productsRef.child(key).setValue(product)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Product Added", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
        }
    }
}
