package com.example.carspa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import android.content.Intent;
import java.io.File;
import java.io.FileOutputStream;
import androidx.core.content.FileProvider;
import android.net.Uri;


public class PdfUtils {
    private List<Item> items;
    public static void createAndSharePdf(Context context, BillModal sale, List<Item> items) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        int y = 25;

        paint.setTextSize(12);
        canvas.drawText("Customer: " + sale.getCustomerName(), 10, y, paint); y += 20;
        canvas.drawText("Bill No: " + sale.getBillNo(), 10, y, paint); y += 20;
        canvas.drawText("Date: " + sale.getBillDate(), 10, y, paint); y += 30;

        paint.setFakeBoldText(true);
        canvas.drawText("Item           Qty     Rate", 10, y, paint); y += 20;
        paint.setFakeBoldText(false);

        double total = 0;
        for (Item item : items) {
            canvas.drawText(item.getName() + "   " + item.getQuantity() + "      " + item.getRate(), 10, y, paint);
            total += item.getQuantity() * item.getRate();
            y += 20;
        }

        y += 10;
        canvas.drawText("Total: ₹" + total, 10, y, paint);

        pdfDocument.finishPage(page);

        File pdfFile = new File(context.getExternalFilesDir(null), "Bill_" + sale.getBillNo() + ".pdf");

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();

            sharePdf(context, pdfFile);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error creating PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private static void sharePdf(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, String.valueOf(uri));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share Bill PDF"));
    }
}
