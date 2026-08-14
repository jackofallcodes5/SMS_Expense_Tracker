package com.example.smsexpensetracker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button btnTransactions;
    private Button btnExpenses;

    private static final String TAG               = "MainActivity";
    private static final int    REQUEST_PERMISSIONS = 101;
    private static final String PREFS_NAME          = "sms_tracker_prefs";
    private static final String PREF_FIRST_LAUNCH   = "first_launch_done";

    // ----- Transaction panel views -----
    private RecyclerView         recyclerView;
    private TransactionAdapter   adapter;
    private LinearLayout         layoutProgress;
    private LinearLayout         layoutEmpty;
    private TextView             tvSummary;
    private FloatingActionButton fabRescan;
    private FloatingActionButton fabAddTransaction;

    // ----- Expense panel views -----
    private RecyclerView         recyclerExpense;
    private ExpenseAdapter       expenseAdapter;
    private LinearLayout         layoutEmptyExpense;
    private TextView             tvTotalBudget;
    private TextView             tvTotalSpent;
    private FloatingActionButton fabAddExpense;

    // ----- Panel containers -----
    private LinearLayout panelTransactions;
    private LinearLayout panelExpenses;

    // ----- Ad -----
    private AdView adView;

    // ----- Data -----
    private DatabaseHelper    dbHelper;
    private List<Transaction> transactionList = new ArrayList<>();
    private List<Expense>     expenseList     = new ArrayList<>();

    /** Single-thread pool for SMS import tasks — avoids deprecated AsyncTask. */
    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    /** Posts results back to the main (UI) thread. */
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    // -------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            Log.e(TAG, "setContentView failed: " + e.getMessage(), e);
            return;
        }

        // Setup toolbar
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) setSupportActionBar(toolbar);
        } catch (Exception e) {
            Log.e(TAG, "Toolbar setup failed: " + e.getMessage(), e);
        }

        dbHelper = new DatabaseHelper(this);

        bindViews();
        setupTransactionRecycler();
        setupExpenseRecycler();
        setupTabButtons();
        initAdMob();

        // Request READ_SMS + RECEIVE_SMS together
        if (hasAllPermissions()) {
            handleFirstLaunch();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS
                    },
                    REQUEST_PERMISSIONS
            );
        }

        // Reload FAB — manually re-scans inbox
        if (fabRescan != null) {
            fabRescan.setOnClickListener(v -> {
                if (hasAllPermissions()) {
                    importSMSInBackground();
                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                            },
                            REQUEST_PERMISSIONS
                    );
                }
            });
        }

        // Add Transaction FAB — opens manual cash entry dialog
        if (fabAddTransaction != null) {
            fabAddTransaction.setOnClickListener(v -> {
                AddTransactionDialog dialog = new AddTransactionDialog(
                        this,
                        dbHelper,
                        () -> {
                            loadFromDatabase();
                            Toast.makeText(this, "Transaction added!", Toast.LENGTH_SHORT).show();
                        }
                );
                dialog.show();
            });
        }

        // Add Expense FAB
        if (fabAddExpense != null) {
            fabAddExpense.setOnClickListener(v -> {
                AddExpenseDialog dialog = new AddExpenseDialog(
                        this,
                        dbHelper,
                        () -> {
                            loadExpenses();
                            Toast.makeText(this, "Expense category added!", Toast.LENGTH_SHORT).show();
                        }
                );
                dialog.show();
            });
        }
    }

    // -------------------------------------------------------
    // onResume — refresh list every time app comes to foreground
    // -------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null && adapter != null) {
            loadFromDatabase();
        }
        if (dbHelper != null && expenseAdapter != null) {
            loadExpenses();
        }
        if (adView != null) adView.resume();
    }

    // -------------------------------------------------------
    // AdMob lifecycle
    // -------------------------------------------------------

    private void initAdMob() {
        MobileAds.initialize(this, initializationStatus ->
                Log.d(TAG, "AdMob initialized."));
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        executor.shutdownNow();
        super.onDestroy();
    }

    // -------------------------------------------------------
    // Permission result
    // -------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean readSmsGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.READ_SMS.equals(permissions[i])
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    readSmsGranted = true;
                }
            }
            if (readSmsGranted) {
                Toast.makeText(this, "Permission granted. Scanning...", Toast.LENGTH_SHORT).show();
                handleFirstLaunch();
            } else {
                Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_LONG).show();
                loadFromDatabase();
            }
        }
    }

    // -------------------------------------------------------
    // First launch — full SMS scan only once
    // -------------------------------------------------------

    private void handleFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean done = prefs.getBoolean(PREF_FIRST_LAUNCH, false);
        if (!done) {
            prefs.edit().putBoolean(PREF_FIRST_LAUNCH, true).apply();
            importSMSInBackground();
        } else {
            loadFromDatabase();
        }
    }

    // -------------------------------------------------------
    // RecyclerView setup
    // -------------------------------------------------------

    private void setupTransactionRecycler() {
        if (recyclerView == null) return;

        adapter = new TransactionAdapter(this, transactionList, transaction -> {
            try {
                TransactionDialog dialog = new TransactionDialog(
                        this,
                        transaction,
                        dbHelper,
                        () -> {
                            loadFromDatabase();
                            loadExpenses();   // refresh expense totals after assignment change
                            Toast.makeText(this, "Transaction updated.", Toast.LENGTH_SHORT).show();
                        }
                );
                dialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Dialog open failed: " + e.getMessage(), e);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void setupExpenseRecycler() {
        if (recyclerExpense == null) return;

        expenseAdapter = new ExpenseAdapter(this, expenseList, this::deleteExpense);
        recyclerExpense.setLayoutManager(new LinearLayoutManager(this));
        recyclerExpense.setAdapter(expenseAdapter);
    }

    private void deleteExpense(Expense expense) {
        if (expense == null) return;
        int rows = dbHelper.deleteExpense(expense);
        if (rows > 0) {
            Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
            loadExpenses();
            loadFromDatabase();
        } else {
            Toast.makeText(this, "Failed to delete expense", Toast.LENGTH_SHORT).show();
        }
    }

    // -------------------------------------------------------
    // Load transactions from SQLite
    // -------------------------------------------------------

    private void loadFromDatabase() {
        try {
            List<Transaction> list = dbHelper.getAllTransactions();
            transactionList.clear();
            transactionList.addAll(list);
            if (adapter != null) adapter.updateData(transactionList);
            updateSummaryBar();
            updateTransactionEmptyState();
        } catch (Exception e) {
            Log.e(TAG, "loadFromDatabase failed: " + e.getMessage(), e);
        }
    }

    private void updateSummaryBar() {
        if (tvSummary == null) return;
        double credit = 0, debit = 0;
        for (Transaction t : transactionList) {
            if ("Credited".equalsIgnoreCase(t.getType()))     credit += t.getAmount();
            else if ("Debited".equalsIgnoreCase(t.getType())) debit  += t.getAmount();
        }
        tvSummary.setText(String.format(
                "%d Transactions  |  + Rs.%.0f  |  - Rs.%.0f",
                transactionList.size(), credit, debit));
    }

    private void updateTransactionEmptyState() {
        if (layoutEmpty == null || recyclerView == null) return;
        if (transactionList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // -------------------------------------------------------
    // Load expenses from SQLite
    // -------------------------------------------------------

    private void loadExpenses() {
        try {
            expenseList.clear();
            expenseList.addAll(dbHelper.getAllExpenses());
            if (expenseAdapter != null) expenseAdapter.updateData(expenseList);
            updateExpenseSummary();
            updateExpenseEmptyState();
        } catch (Exception e) {
            Log.e(TAG, "loadExpenses failed: " + e.getMessage(), e);
        }
    }

    private void updateExpenseSummary() {
        if (tvTotalBudget == null || tvTotalSpent == null) return;
        double budget = 0, spent = 0;
        for (Expense ex : expenseList) {
            budget += ex.getMonthlyBudget();
            spent  += ex.getSpentAmount();
        }
        tvTotalBudget.setText(String.format("Budget : ₹%.0f", budget));
        tvTotalSpent.setText(String.format("Spent : ₹%.0f", spent));
    }

    private void updateExpenseEmptyState() {
        if (layoutEmptyExpense == null || recyclerExpense == null) return;
        if (expenseList.isEmpty()) {
            layoutEmptyExpense.setVisibility(View.VISIBLE);
            recyclerExpense.setVisibility(View.GONE);
        } else {
            layoutEmptyExpense.setVisibility(View.GONE);
            recyclerExpense.setVisibility(View.VISIBLE);
        }
    }

    // -------------------------------------------------------
    // View binding
    // -------------------------------------------------------

    private void bindViews() {
        // Panels
        panelTransactions = findViewById(R.id.panelTransactions);
        panelExpenses     = findViewById(R.id.panelExpenses);

        // Transaction panel
        recyclerView      = findViewById(R.id.recyclerView);
        layoutProgress    = findViewById(R.id.progressBar);
        layoutEmpty       = findViewById(R.id.tvEmptyState);
        tvSummary         = findViewById(R.id.tvSummary);
        fabRescan         = findViewById(R.id.fabRescan);
        fabAddTransaction = findViewById(R.id.fabAddTransaction);

        // Expense panel
        recyclerExpense    = findViewById(R.id.recyclerExpense);
        layoutEmptyExpense = findViewById(R.id.layoutEmptyExpense);
        tvTotalBudget      = findViewById(R.id.tvTotalBudget);
        tvTotalSpent       = findViewById(R.id.tvTotalSpent);
        fabAddExpense      = findViewById(R.id.fabAddExpense);

        // Tabs
        btnTransactions = findViewById(R.id.btnTransactions);
        btnExpenses     = findViewById(R.id.btnExpenses);

        // Ad
        adView = findViewById(R.id.adView);
    }

    // -------------------------------------------------------
    // Permission helpers
    // -------------------------------------------------------

    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // -------------------------------------------------------
    // TAB NAVIGATION
    // -------------------------------------------------------

    private void setupTabButtons() {
        if (btnTransactions == null || btnExpenses == null) return;

        // Start on Transactions tab (already visible by default in XML)
        setActiveTab(true);

        btnTransactions.setOnClickListener(v -> {
            if (panelTransactions.getVisibility() == View.VISIBLE) return; // already here
            setActiveTab(true);
        });

        btnExpenses.setOnClickListener(v -> {
            if (panelExpenses.getVisibility() == View.VISIBLE) return; // already here
            loadExpenses(); // always refresh when switching in
            setActiveTab(false);
        });
    }

    /**
     * @param transactionsActive true → show Transactions panel; false → show Expenses panel
     */
    private void setActiveTab(boolean transactionsActive) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        int inactiveBgColor = typedValue.data;
        int activeBgColor = ContextCompat.getColor(this, R.color.colorPrimary);

        if (transactionsActive) {
            // Show transactions panel
            panelTransactions.setVisibility(View.VISIBLE);
            panelExpenses.setVisibility(View.GONE);

            // Show transaction FABs, hide expense FAB
            if (fabRescan != null)         fabRescan.setVisibility(View.VISIBLE);
            if (fabAddTransaction != null) fabAddTransaction.setVisibility(View.VISIBLE);
            if (fabAddExpense != null)     fabAddExpense.setVisibility(View.GONE);

            // Style active/inactive buttons
            btnTransactions.setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
            btnTransactions.setTextColor(Color.WHITE);

            btnExpenses.setBackgroundTintList(ColorStateList.valueOf(inactiveBgColor));
            btnExpenses.setTextColor(activeBgColor);

        } else {
            // Show expenses panel
            panelExpenses.setVisibility(View.VISIBLE);
            panelTransactions.setVisibility(View.GONE);

            // Show expense FAB, hide transaction FABs
            if (fabAddExpense != null)     fabAddExpense.setVisibility(View.VISIBLE);
            if (fabRescan != null)         fabRescan.setVisibility(View.GONE);
            if (fabAddTransaction != null) fabAddTransaction.setVisibility(View.GONE);

            // Style active/inactive buttons
            btnExpenses.setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
            btnExpenses.setTextColor(Color.WHITE);

            btnTransactions.setBackgroundTintList(ColorStateList.valueOf(inactiveBgColor));
            btnTransactions.setTextColor(activeBgColor);
        }
    }

    // -------------------------------------------------------
    // Background SMS import
    // -------------------------------------------------------

    private void importSMSInBackground() {
        // Show progress on UI thread
        if (layoutProgress != null)    layoutProgress.setVisibility(View.VISIBLE);
        if (fabRescan != null)         fabRescan.setEnabled(false);
        if (fabAddTransaction != null) fabAddTransaction.setEnabled(false);
        if (layoutEmpty != null)       layoutEmpty.setVisibility(View.GONE);

        executor.execute(() -> {
            int newRows = 0;
            try {
                List<Transaction> parsed =
                        SMSReader.readBankingTransactions(MainActivity.this);
                newRows = dbHelper.insertTransactions(parsed);
            } catch (Exception e) {
                Log.e(TAG, "importSMSInBackground error: " + e.getMessage(), e);
            }

            final int result = newRows;

            mainHandler.post(() -> {
                if (layoutProgress != null)    layoutProgress.setVisibility(View.GONE);
                if (fabRescan != null)         fabRescan.setEnabled(true);
                if (fabAddTransaction != null) fabAddTransaction.setEnabled(true);
                loadFromDatabase();
                String msg = result > 0
                        ? result + " new transaction(s) imported!"
                        : "No new transactions found.";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            });
        });
    }
}