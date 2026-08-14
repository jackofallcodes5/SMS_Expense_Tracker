package com.example.smsexpensetracker;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


public class TransactionDialog extends Dialog {


    public interface OnDescriptionSavedListener {
        void onSaved();
    }


    private final Transaction      transaction;
    private final DatabaseHelper   db;
    private final OnDescriptionSavedListener listener;


    private TextView tvDialogDatetime;
    private TextView tvDialogAmount;
    private TextView tvDialogType;
    private TextView tvDialogParty;
    private TextView tvDialogReference;

    private EditText etDialogDescription;

    private Spinner  spinnerExpense;
    private TextView tvExpenseLabel;

    private Button btnSave;
    private Button btnClose;


    /** Full list of expense names including the placeholder at position 0. */
    private List<String> expenseNames = new ArrayList<>();

    /** Position 0 is always this non-saving placeholder. */
    private static final String PLACEHOLDER = "Select category";


    public TransactionDialog(
            @NonNull Context context,
            @NonNull Transaction transaction,
            @NonNull DatabaseHelper db,
            OnDescriptionSavedListener listener) {

        super(context);
        this.transaction = transaction;
        this.db          = db;
        this.listener    = listener;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setContentView(R.layout.dialog_transaction);

        bindViews();
        populateData();
        setupExpenseSpinner();
        setClickListeners();
    }


    private void bindViews() {
        tvDialogDatetime  = findViewById(R.id.tvDialogDatetime);
        tvDialogAmount    = findViewById(R.id.tvDialogAmount);
        tvDialogType      = findViewById(R.id.tvDialogType);
        tvDialogParty     = findViewById(R.id.tvDialogParty);
        tvDialogReference = findViewById(R.id.tvDialogReference);
        etDialogDescription = findViewById(R.id.etDialogDescription);
        spinnerExpense    = findViewById(R.id.spinnerExpense);
        tvExpenseLabel    = findViewById(R.id.tvExpenseLabel);
        btnSave           = findViewById(R.id.btnSave);
        btnClose          = findViewById(R.id.btnClose);
    }


    private void populateData() {

        tvDialogDatetime.setText(transaction.getDatetime());

        tvDialogAmount.setText(
                String.format("₹ %.2f", transaction.getAmount()));

        String type = transaction.getType();
        tvDialogType.setText(type);

        if ("Credited".equalsIgnoreCase(type)) {
            tvDialogType.setTextColor(Color.parseColor("#2E7D32"));
        } else if ("Debited".equalsIgnoreCase(type)) {
            tvDialogType.setTextColor(Color.parseColor("#C62828"));
        }

        tvDialogParty.setText(
                TextUtils.isEmpty(transaction.getParty()) ? "N/A" : transaction.getParty());

        tvDialogReference.setText(
                TextUtils.isEmpty(transaction.getReference()) ? "N/A" : transaction.getReference());

        etDialogDescription.setText(transaction.getDescription());
        etDialogDescription.setSelection(etDialogDescription.length());
    }


    private void setupExpenseSpinner() {

        // Only debit transactions can be assigned to an expense
        if (!"Debited".equalsIgnoreCase(transaction.getType())) {
            spinnerExpense.setVisibility(android.view.View.GONE);
            tvExpenseLabel.setVisibility(android.view.View.GONE);
            return;
        }

        spinnerExpense.setVisibility(android.view.View.VISIBLE);
        tvExpenseLabel.setVisibility(android.view.View.VISIBLE);

        // Build list: placeholder first, then real names
        expenseNames = new ArrayList<>();
        expenseNames.add(PLACEHOLDER);
        expenseNames.addAll(db.getExpenseNames());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                expenseNames
        );
        spinnerExpense.setAdapter(adapter);

        // Pre-select the currently assigned expense (if any)
        int currentExpenseId = transaction.getExpenseId();
        if (currentExpenseId != -1) {
            List<Expense> all = db.getAllExpenses();
            for (Expense e : all) {
                if (e.getId() == currentExpenseId) {
                    int pos = expenseNames.indexOf(e.getName());
                    if (pos >= 0) spinnerExpense.setSelection(pos);
                    break;
                }
            }
        }
    }


    private void setClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveTransaction());
    }


    private void saveTransaction() {

        String description = etDialogDescription.getText().toString().trim();

        if (TextUtils.isEmpty(description)) {
            Toast.makeText(getContext(), "Description cannot be empty",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Save description
        int rows = db.updateDescription(transaction.getId(), description);
        transaction.setDescription(description);

        // -------------------------------------------------------
        // Expense assignment with reversal logic
        // -------------------------------------------------------
        if ("Debited".equalsIgnoreCase(transaction.getType())) {

            String selected = spinnerExpense.getSelectedItem().toString();

            // The expense_id that was stored in the DB before this dialog opened
            int oldExpenseId = db.getExpenseIdForTransaction(transaction.getId());

            if (selected.equals(PLACEHOLDER)) {
                // User chose "Select category" — clear any existing assignment
                if (oldExpenseId != -1) {
                    db.subtractExpenseSpent(oldExpenseId, transaction.getAmount());
                    db.clearTransactionExpense(transaction.getId());
                    transaction.setExpenseId(-1);
                }
            } else {
                // User chose a real expense category
                int newExpenseId = getExpenseId(selected);

                if (newExpenseId != -1) {
                    if (oldExpenseId == newExpenseId) {
                        // Same category — no financial change needed, just ensure link is set
                        db.updateTransactionExpense(transaction.getId(), newExpenseId);
                    } else {
                        // Different or newly assigned category
                        // 1. Reverse the old category's spent amount
                        if (oldExpenseId != -1) {
                            db.subtractExpenseSpent(oldExpenseId, transaction.getAmount());
                        }
                        // 2. Add to the new category
                        db.updateTransactionExpense(transaction.getId(), newExpenseId);
                        db.addExpenseSpent(newExpenseId, transaction.getAmount());
                        transaction.setExpenseId(newExpenseId);
                    }
                }
            }
        }

        if (rows > 0) {
            Toast.makeText(getContext(), "Transaction updated", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onSaved();
            dismiss();
        } else {
            Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
        }
    }


    private int getExpenseId(String name) {
        for (Expense e : db.getAllExpenses()) {
            if (e.getName().equals(name)) return e.getId();
        }
        return -1;
    }
}