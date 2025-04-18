package com.example.carspa;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        Button addInvoiceBtn = findViewById(R.id.add_invoice_button);
        addInvoiceBtn.setOnClickListener(v -> showAddExpenseDialog());

        // Button to add sale
        Button btnAddSale = findViewById(R.id.add_sale_button);
        Button btnAddInvoice = findViewById(R.id.add_invoice_button);
        BarChart barChart = findViewById(R.id.barChart);

// Sample data
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 50));
        entries.add(new BarEntry(1, 80));
        entries.add(new BarEntry(2, 60));
        entries.add(new BarEntry(3, 30));
        entries.add(new BarEntry(4, 90));

        BarDataSet dataSet = new BarDataSet(entries, "Sales");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

// Optional: Remove description label
        Description description = new Description();
        description.setText("");
        barChart.setDescription(description);

// Refresh
        barChart.invalidate();
        btnAddSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SaleActivity.class);
                startActivity(intent);
            }
        });

        btnAddInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddExpenseDialog(); // Open the expense modal
            }
        });
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        builder.setView(view);

        EditText etInvoiceNo = view.findViewById(R.id.et_invoice_no);
        EditText etDescription = view.findViewById(R.id.et_description);
        EditText etAmount = view.findViewById(R.id.et_amount);
        Button btnSave = view.findViewById(R.id.btn_save_expense);

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String invoiceNo = etInvoiceNo.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            if (!invoiceNo.isEmpty() && !description.isEmpty() && !amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);

                // TODO: Save the expense to your database here
                // db.insertExpense(invoiceNo, description, amount);

                Toast.makeText(MainActivity.this, "Expense added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }



    // Bottom Navigation Listener
    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();

                    if (id == R.id.nav_home) {
                        // You can add home logic here if needed
                        return true;
                    } else if (id == R.id.nav_profile) {
                        // Open BillListActivity when profile is clicked
                        Intent intent = new Intent(MainActivity.this, BillListActivity.class);
                        startActivity(intent);
                        return true;
                    } else if (id == R.id.nav_settings) {
                        Intent intent = new Intent(MainActivity.this, AddProductActivity.class);
                        startActivity(intent);
                        return true;
                    }

                    return false;
                }
            };
}
