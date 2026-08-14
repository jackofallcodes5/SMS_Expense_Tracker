package com.example.smsexpensetracker;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;


public class ExpenseActivity extends AppCompatActivity {


    private RecyclerView recyclerExpense;

    private ExpenseAdapter adapter;

    private DatabaseHelper db;


    private TextView tvTotalBudget;
    private TextView tvTotalSpent;


    private LinearLayout layoutEmptyExpense;


    private FloatingActionButton fabAddExpense;



    private List<Expense> expenseList = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_expenses);



        Toolbar toolbar =
                findViewById(R.id.toolbarExpenses);

        setSupportActionBar(toolbar);



        db = new DatabaseHelper(this);



        bindViews();


        setupRecycler();


        loadExpenses();



        fabAddExpense.setOnClickListener(v -> {


            AddExpenseDialog dialog =
                    new AddExpenseDialog(
                            this,
                            db,
                            this::loadExpenses
                    );


            dialog.show();

        });

    }



    private void bindViews() {


        recyclerExpense =
                findViewById(R.id.recyclerExpense);


        tvTotalBudget =
                findViewById(R.id.tvTotalBudget);


        tvTotalSpent =
                findViewById(R.id.tvTotalSpent);


        layoutEmptyExpense =
                findViewById(R.id.layoutEmptyExpense);


        fabAddExpense =
                findViewById(R.id.fabAddExpense);

    }





    private void setupRecycler() {


        recyclerExpense.setLayoutManager(
                new LinearLayoutManager(this)
        );


        adapter =
                new ExpenseAdapter(
                        this,
                        expenseList,
                        this::deleteExpense
                );


        recyclerExpense.setAdapter(adapter);

    }




    private void deleteExpense(Expense expense) {

        int rows = db.deleteExpense(expense);

        if (rows > 0) {

            Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();

            loadExpenses();

        } else {

            Toast.makeText(this, "Failed to delete expense", Toast.LENGTH_SHORT).show();

        }

    }




    private void loadExpenses() {


        expenseList.clear();


        expenseList.addAll(
                db.getAllExpenses()
        );


        adapter.updateData(expenseList);



        updateSummary();


        if(expenseList.isEmpty()){


            layoutEmptyExpense.setVisibility(
                    LinearLayout.VISIBLE
            );


            recyclerExpense.setVisibility(
                    RecyclerView.GONE
            );


        }else{


            layoutEmptyExpense.setVisibility(
                    LinearLayout.GONE
            );


            recyclerExpense.setVisibility(
                    RecyclerView.VISIBLE
            );

        }

    }


    private void updateSummary() {


        double budget = 0;

        double spent = 0;



        for(Expense expense : expenseList){


            budget += expense.getMonthlyBudget();

            spent += expense.getSpentAmount();

        }



        tvTotalBudget.setText(
                String.format(
                        "Budget : ₹%.0f",
                        budget
                )
        );



        tvTotalSpent.setText(
                String.format(
                        "Spent : ₹%.0f",
                        spent
                )
        );

    }

}