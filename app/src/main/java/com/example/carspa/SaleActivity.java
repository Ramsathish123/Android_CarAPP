package com.example.carspa;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class SaleActivity extends AppCompatActivity {

    private EditText billDate, billNo,customerPhone,customerName,nxtDate,description;
    private RecyclerView itemsRecyclerView;
    private Button addItemsButton, saveButton;

    private TextView totalAmountText,receivedAmountText,balanceAmountText,discountAmountText;

    private AutoCompleteTextView itemName;
    private List<Item> itemList;
    private ItemAdapter itemAdapter;
    private DatabaseReference saleRef, saleItemRef;

    private String saleId = null;
    private double totalAmount = 0.0;
    private final ArrayList<String> itemNames = new ArrayList<>();
    private final HashMap<String, Product> productMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale);

        billDate = findViewById(R.id.billDate);
        billNo = findViewById(R.id.billNo);
        customerName=findViewById(R.id.customerName);
        customerPhone=findViewById(R.id.customerPhone);
        totalAmountText=findViewById(R.id.totalAmountText);
        receivedAmountText=findViewById(R.id.receivedAmountText);
        balanceAmountText=findViewById(R.id.balanceAmountText);
        addItemsButton = findViewById(R.id.addItemsButton);
        saveButton = findViewById(R.id.saveButton);
        itemsRecyclerView = findViewById(R.id.itemsRecyclerView);
        nxtDate=findViewById(R.id.nxtDate);
        description=findViewById(R.id.description);
        discountAmountText=findViewById(R.id.discountAmountText);

        saleRef = FirebaseDatabase.getInstance().getReference("Sales");
        saleItemRef = FirebaseDatabase.getInstance().getReference("SaleItems");

        itemList = new ArrayList<>();
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        itemAdapter = new ItemAdapter(itemList, this::showEditItemDialog);
        itemsRecyclerView.setAdapter(itemAdapter);


        saleId = getIntent().getStringExtra("saleId");
        if (saleId != null) {
            loadSaleDetails(saleId);
        }
        Button btnForward = findViewById(R.id.forwardButton);
        btnForward.setOnClickListener(v -> generateAndSharePdf());

        addItemsButton.setOnClickListener(view -> showAddItemDialog());
        saveButton.setOnClickListener(view -> saveSale());

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        billDate.setText(dateFormat.format(calendar.getTime()));
        nxtDate.setText(dateFormat.format(calendar.getTime()));
        billDate.setOnClickListener(v -> {
            //Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(SaleActivity.this, (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
               // SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                billDate.setText(dateFormat.format(selectedDate.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        nxtDate.setOnClickListener(v -> {
            //Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(SaleActivity.this, (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                // SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                billDate.setText(dateFormat.format(selectedDate.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        if (getIntent().hasExtra("saleId")) {
            saleId = getIntent().getStringExtra("saleId");
            loadSaleDetails(saleId); // don't generate bill number if editing
        } else {
            generateNextBillNo(); // only auto-generate if creating new bill
        }

        receivedAmountText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                updateBalanceAmount();
            }
        });

    }

    private void loadSaleDetails(String saleId) {
        saleRef.child(saleId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    billNo.setText(snapshot.child("billNo").getValue(String.class));
                    billDate.setText(snapshot.child("billDate").getValue(String.class));
                    customerName.setText(snapshot.child("customerName").getValue(String.class));
                    customerPhone.setText(snapshot.child("customerPhone").getValue(String.class));
                    receivedAmountText.setText(snapshot.child("receivedAmountText").getValue(String.class));
                    balanceAmountText.setText(snapshot.child("balanceAmountText").getValue(String.class));
                    description.setText(snapshot.child("description").getValue(String.class));
                    nxtDate.setText(snapshot.child("nxtDate").getValue(String.class));
                    totalAmount = snapshot.child("totalAmount").getValue(Double.class);
                    updateTotalAmountDisplay();
                    loadSaleItems(saleId);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SaleActivity.this, "Error loading sale details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSaleItems(String saleId) {
        saleItemRef.orderByChild("saleId").equalTo(saleId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String name = itemSnapshot.child("itemName").getValue(String.class);
                    int quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                    double rate = itemSnapshot.child("rate").getValue(Double.class);
                    itemList.add(new Item(name, quantity, rate));
                }
                itemAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SaleActivity.this, "Error loading items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.item_modal, null);
        builder.setView(dialogView);

        AutoCompleteTextView itemName = dialogView.findViewById(R.id.itemName);
        EditText itemQuantity = dialogView.findViewById(R.id.itemQuantity);
        EditText itemRate = dialogView.findViewById(R.id.itemRate);
        Button addItemButton = dialogView.findViewById(R.id.addItemButton);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Fetch products from Firebase and bind to dropdown
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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

                ArrayAdapter<String> adapter = new ArrayAdapter<>(SaleActivity.this,
                        android.R.layout.simple_dropdown_item_1line, itemNames);
                itemName.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SaleActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });

        // Populate quantity and rate on item select
        itemName.setOnItemClickListener((adapterView, view, i, l) -> {
            String selectedName = adapterView.getItemAtPosition(i).toString();
            Product selectedProduct = productMap.get(selectedName);

            if (selectedProduct != null) {
               // itemQuantity.setText(String.valueOf(selectedProduct.quantity));
                itemRate.setText(String.valueOf(selectedProduct.rate));
            }
        });

        // Handle adding the item
        addItemButton.setOnClickListener(view -> {
            String name = itemName.getText().toString().trim();
            String quantityStr = itemQuantity.getText().toString().trim();
            String rateStr = itemRate.getText().toString().trim();

            if (name.isEmpty() || quantityStr.isEmpty() || rateStr.isEmpty()) {
                Toast.makeText(SaleActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            double rate = Double.parseDouble(rateStr);
            double total = quantity * rate;

            itemList.add(new Item(name, quantity, rate));
            totalAmount += total;
            updateTotalAmountDisplay();
            itemAdapter.notifyDataSetChanged();
            dialog.dismiss();
        });
    }

    private void showEditItemDialog(int position) {
        Item item = itemList.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.item_modal, null);
        builder.setView(dialogView);

        EditText itemName = dialogView.findViewById(R.id.itemName);
        EditText itemQuantity = dialogView.findViewById(R.id.itemQuantity);
        EditText itemRate = dialogView.findViewById(R.id.itemRate);
        Button addItemButton = dialogView.findViewById(R.id.addItemButton);
        addItemButton.setText("Update");

        itemName.setText(item.getName());
        itemQuantity.setText(String.valueOf(item.getQuantity()));
        itemRate.setText(String.valueOf(item.getRate()));

        AlertDialog dialog = builder.create();
        dialog.show();

        addItemButton.setOnClickListener(view -> {
            String name = itemName.getText().toString().trim();
            String quantityStr = itemQuantity.getText().toString().trim();
            String rateStr = itemRate.getText().toString().trim();

            if (name.isEmpty() || quantityStr.isEmpty() || rateStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            double rate = Double.parseDouble(rateStr);
            double total = quantity * rate;

            // Update total by subtracting old and adding new
            totalAmount -= item.getQuantity() * item.getRate();
            totalAmount += total;
            updateTotalAmountDisplay();
            item.setName(name);
            item.setQuantity(quantity);
            item.setRate(rate);

            itemAdapter.notifyItemChanged(position);
            dialog.dismiss();
        });
    }

    private void saveSale() {
        String billNoStr = billNo.getText().toString().trim();
        String billDateStr = billDate.getText().toString().trim();
        String cusName=customerName.getText().toString().trim();
        String cusMob=customerPhone.getText().toString().trim();
        String recAmt=receivedAmountText.getText().toString().trim();
        String balAmt=balanceAmountText.getText().toString().trim();
        String nextDate=nxtDate.getText().toString().trim();
        String desc=description.getText().toString().trim();


        if (billNoStr.isEmpty() || billDateStr.isEmpty() || itemList.isEmpty()) {
            Toast.makeText(this, "Fill all details and add at least one item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (saleId == null) {
            saleId = saleRef.push().getKey();
        }

        Map<String, Object> saleData = new HashMap<>();
        saleData.put("billNo", billNoStr);
        saleData.put("billDate", billDateStr);
        saleData.put("customerName", cusName);
        saleData.put("customerPhone", cusMob);
        saleData.put("totalAmount", totalAmount);
        saleData.put("receivedAmountText", recAmt);
        saleData.put("balanceAmountText", balAmt);
        saleData.put("nxtDate", nextDate);
        saleData.put("description", desc);

        saleRef.child(saleId).setValue(saleData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                saveSaleItems(saleId);
            } else {
                Toast.makeText(this, "Error saving sale", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSaleItems(String saleId) {
        saleItemRef.orderByChild("saleId").equalTo(saleId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    itemSnapshot.getRef().removeValue();
                }

                for (Item item : itemList) {
                    String itemId = saleItemRef.push().getKey();
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("saleId", saleId);
                    itemData.put("itemName", item.getName());
                    itemData.put("quantity", item.getQuantity());
                    itemData.put("rate", item.getRate());
                    itemData.put("total", item.getQuantity() * item.getRate());

                    saleItemRef.child(itemId).setValue(itemData);
                }

                Toast.makeText(SaleActivity.this, "Sale saved successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SaleActivity.this, BillListActivity.class));
                finish();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SaleActivity.this, "Error saving sale items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateAndSharePdf() {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            String fileName = "Bill_" + billNo.getText().toString() + ".pdf";
            File directory = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Bills");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, fileName);

            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            BaseColor deepPink = new BaseColor(255, 20, 147);

            // Fonts
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, deepPink);
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font totalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font regularFont = new Font(Font.FontFamily.HELVETICA, 10);

            // Header Table: Company Details + Logo
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new int[]{3, 1});

            // Company Info
            Paragraph company = new Paragraph();
            company.add(new Chunk("Athiran Car Care\n", boldFont)); // Bold company name
            company.add(new Chunk(
                    "12, Green Park, thittai corner, Kumbakonam Main Road,\n" +
                            "Palliagraharam, Thanjavur - 613003\n" +
                            "Phone: 9556712333\n" +
                            "Email: sundar.tech1991@gmail.com",
                    normalFont
            ));
            PdfPCell companyCell = new PdfPCell(company);
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(companyCell);


            // Load Logo from assets
            try {
                InputStream ims = getAssets().open("car1.jpg");
                Bitmap bitmap = BitmapFactory.decodeStream(ims);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                Image logo = Image.getInstance(stream.toByteArray());
                logo.scaleToFit(80, 80);

                PdfPCell logoCell = new PdfPCell(logo, false);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                logoCell.setVerticalAlignment(Element.ALIGN_TOP);
                headerTable.addCell(logoCell);
            } catch (Exception e) {
                e.printStackTrace();
                headerTable.addCell(new PdfPCell(new Phrase(""))); // fallback
            }

            document.add(headerTable);

            // Horizontal line
            LineSeparator line = new LineSeparator();
            line.setLineColor(BaseColor.LIGHT_GRAY);
            line.setLineWidth(0.5f);
            document.add(new Chunk(line));

            // Tax Invoice Title
            Paragraph title = new Paragraph("Tax Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            title.setSpacingAfter(10);
            document.add(title);

            // Invoice Info Table
            PdfPTable invoiceTable = new PdfPTable(2);
            invoiceTable.setWidthPercentage(100);
            invoiceTable.setSpacingBefore(10);

            // Bill To (Left)
            Phrase billTo = new Phrase();
            billTo.add(new Chunk("Bill To:\n", labelFont));
            billTo.add(new Chunk(customerName.getText().toString() + "\nContact: " + customerPhone.getText().toString(), normalFont));
            invoiceTable.addCell(getCell(billTo, PdfPCell.ALIGN_LEFT));

            // Invoice Info (Right)
            Phrase invoiceDetails = new Phrase();
            invoiceDetails.add(new Chunk("Invoice No: ", labelFont));
            invoiceDetails.add(new Chunk(billNo.getText().toString() + "\n", normalFont));
            invoiceDetails.add(new Chunk("Date: ", labelFont));
            invoiceDetails.add(new Chunk(billDate.getText().toString(), normalFont));
            invoiceTable.addCell(getCell(invoiceDetails, PdfPCell.ALIGN_RIGHT));

            document.add(invoiceTable);

            // Item Table
            PdfPTable itemTable = new PdfPTable(new float[]{1, 4, 2, 2, 2});
            itemTable.setWidthPercentage(100);
            itemTable.setSpacingBefore(20);

            addTableHeader(itemTable, new String[]{"#", "Products", "Quantity", "Rate", "Amount"}, deepPink, totalFont);

            int index = 1;
            double total = 0;
            for (Item item : itemList) {
                double amount = item.getQuantity() * item.getRate();
                total += amount;

                itemTable.addCell(String.valueOf(index++));
                itemTable.addCell(item.getName());
                itemTable.addCell(String.valueOf(item.getQuantity()));
                itemTable.addCell("₹" + item.getRate());
                itemTable.addCell("₹" + amount);
            }

            document.add(itemTable);

            String receivedText = receivedAmountText.getText().toString().trim();
            String discountText = discountAmountText.getText().toString().trim(); // assuming discount field

            double discount = 0;
            double receivedAmount = 0;
            double pendingAmount = 0;

            if (!discountText.isEmpty()) {
                discount = Double.parseDouble(discountText);
                total -= discount; // apply discount
                pendingAmount = total;
            }

            if (!receivedText.isEmpty()) {
                receivedAmount = Double.parseDouble(receivedText);
                pendingAmount = total - receivedAmount;
            }


            // Parent 2-column layout: Left = Description, Right = Amount Table
            PdfPTable layoutTable = new PdfPTable(2);
            layoutTable.setWidthPercentage(100);
            layoutTable.setWidths(new float[]{2f, 1.5f});
            layoutTable.setSpacingBefore(10);
            layoutTable.setSpacingAfter(5);

// Description Cell
            Phrase phrase = new Phrase();
            phrase.add(new Chunk("Description:\n", boldFont));
            phrase.add(new Chunk(description.getText().toString(), normalFont)); // Assuming `description` is the EditText

            PdfPCell descCell = new PdfPCell(phrase);
            descCell.setBorder(Rectangle.NO_BORDER);
            descCell.setVerticalAlignment(Element.ALIGN_TOP);
            descCell.setPaddingRight(10);

// Amount Table (same as before)
            PdfPTable amountTable = new PdfPTable(2);
            amountTable.setWidths(new int[]{1, 2});
            amountTable.setWidthPercentage(100); // match parent
            amountTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            amountTable.addCell(getCell("Sub Total", PdfPCell.ALIGN_LEFT, boldFont));
            amountTable.addCell(getCell("₹" + total, PdfPCell.ALIGN_RIGHT, regularFont));

// TOTAL row with background
            PdfPCell totalLabel = new PdfPCell(new Phrase("Total", boldFont));
            totalLabel.setBackgroundColor(new BaseColor(220, 0, 120));
            totalLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            totalLabel.setPadding(5);

            PdfPCell totalValue = new PdfPCell(new Phrase("₹" + total, boldFont));
            totalValue.setBackgroundColor(new BaseColor(220, 0, 120));
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValue.setPadding(5);

            amountTable.addCell(totalLabel);
            amountTable.addCell(totalValue);

            amountTable.addCell(getCell("Received", PdfPCell.ALIGN_LEFT, boldFont));
            amountTable.addCell(getCell("₹" + receivedAmount, PdfPCell.ALIGN_RIGHT, regularFont));

            amountTable.addCell(getCell("Balance", PdfPCell.ALIGN_LEFT, boldFont));
            amountTable.addCell(getCell("₹" + pendingAmount, PdfPCell.ALIGN_RIGHT, regularFont));

// Add both to layout table
            PdfPCell amountCell = new PdfPCell(amountTable);
            amountCell.setBorder(Rectangle.NO_BORDER);

            layoutTable.addCell(descCell);
            layoutTable.addCell(amountCell);

            document.add(layoutTable);

            // Amount in Words
            document.add(new Paragraph("Amount in Words: " + convertNumberToWords((int) total) + " Rupees only", normalFont));

            // Terms
            Paragraph terms = new Paragraph("Terms and Conditions:\nThank you for doing business with us.", normalFont);
            terms.setSpacingBefore(20);
            document.add(terms);

            document.close();
            sharePdf(file);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private String convertNumberToWords(int number) {
        if (number == 0) return "Zero";

        String[] units = {
                "", "One", "Two", "Three", "Four", "Five",
                "Six", "Seven", "Eight", "Nine", "Ten", "Eleven",
                "Twelve", "Thirteen", "Fourteen", "Fifteen",
                "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        };

        String[] tens = {
                "", "", "Twenty", "Thirty", "Forty", "Fifty",
                "Sixty", "Seventy", "Eighty", "Ninety"
        };

        StringBuilder words = new StringBuilder();

        if ((number / 100000) > 0) {
            words.append(units[number / 100000]).append(" Lakh ");
            number %= 100000;
        }

        if ((number / 1000) > 0) {
            words.append(convertNumberToWords(number / 1000)).append(" Thousand ");
            number %= 1000;
        }

        if ((number / 100) > 0) {
            words.append(units[number / 100]).append(" Hundred ");
            number %= 100;
        }

        if (number > 0) {
            if (number < 20) {
                words.append(units[number]);
            } else {
                words.append(tens[number / 10]);
                if ((number % 10) > 0) {
                    words.append(" ").append(units[number % 10]);
                }
            }
        }

        return words.toString().trim();
    }

    private PdfPCell getCell(String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(PdfPCell.NO_BORDER);
        return cell;
    }

    private PdfPCell getCell(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(PdfPCell.NO_BORDER);
        return cell;
    }

    private void addTableHeader(PdfPTable table, String[] headers, BaseColor bgColor, Font font) {
        for (String columnTitle : headers) {
            PdfPCell header = new PdfPCell();
            header.setBackgroundColor(bgColor);
            header.setPhrase(new Phrase(columnTitle, font));
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setPadding(5);
            table.addCell(header);
        }
    }

    private void sharePdf(File file) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Bill"));
    }

    private void generateNextBillNo() {
        if (saleId != null) return; // skip if editing

        saleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int nextBillNo = (int) snapshot.getChildrenCount() + 1;
                billNo.setText(String.valueOf(nextBillNo));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SaleActivity.this, "Failed to generate bill number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBalanceAmount() {
        String receivedStr = receivedAmountText.getText().toString().trim();

        double received = 0;
        if (!receivedStr.isEmpty()) {
            try {
                received = Double.parseDouble(receivedStr);
            } catch (NumberFormatException e) {
                received = 0;
            }
        }

        double balance = totalAmount - received;
        if (received > totalAmount) {
            Toast.makeText(this, "Received amount is greater than total amount!", Toast.LENGTH_SHORT).show();
            balance = 0; // Optional: You can choose to show negative balance instead
        }
        if (balance < 0) balance = 0;

        balanceAmountText.setText(String.format(Locale.getDefault(), "₹%.2f", balance));
    }

    private void updateTotalAmountDisplay() {
        totalAmountText.setText(String.format(Locale.getDefault(), "Total: ₹%.2f", totalAmount));
    }


}
