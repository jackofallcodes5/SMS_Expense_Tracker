package com.example.smsexpensetracker;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import androidx.annotation.NonNull;



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class AddExpenseDialog extends Dialog {



    private final DatabaseHelper db;

    private final Runnable refresh;



    private EditText etExpenseName;
    private EditText etExpenseBudget;


    private Button btnSave;
    private Button btnCancel;




    public AddExpenseDialog(
            @NonNull Context context,
            DatabaseHelper db,
            Runnable refresh) {


        super(context);

        this.db = db;

        this.refresh = refresh;

    }





    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);



        requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );


        if(getWindow()!=null){

            getWindow()
                    .setBackgroundDrawable(
                            new ColorDrawable(Color.TRANSPARENT)
                    );
        }



        setContentView(
                R.layout.dialog_add_expense
        );



        bindViews();


        setListeners();

    }





    private void bindViews(){


        etExpenseName =
                findViewById(R.id.etExpenseName);


        etExpenseBudget =
                findViewById(R.id.etExpenseBudget);



        btnSave =
                findViewById(R.id.btnSaveExpense);


        btnCancel =
                findViewById(R.id.btnCancelExpense);

    }







    private void setListeners(){



        btnCancel.setOnClickListener(v -> dismiss());




        btnSave.setOnClickListener(v -> saveExpense());

    }






    private void saveExpense(){



        String name =
                etExpenseName.getText()
                        .toString()
                        .trim();



        String budgetText =
                etExpenseBudget.getText()
                        .toString()
                        .trim();




        if(TextUtils.isEmpty(name)){


            Toast.makeText(
                    getContext(),
                    "Enter expense name",
                    Toast.LENGTH_SHORT
            ).show();


            return;

        }




        if(TextUtils.isEmpty(budgetText)){


            Toast.makeText(
                    getContext(),
                    "Enter budget amount",
                    Toast.LENGTH_SHORT
            ).show();


            return;

        }




        double budget =
                Double.parseDouble(
                        budgetText
                );




        String date =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                ).format(
                        new Date()
                );




        Expense expense =
                new Expense(
                        name,
                        budget,
                        0,
                        date
                );




        long id =
                db.insertExpense(
                        expense
                );




        if(id > 0){


            Toast.makeText(
                    getContext(),
                    "Expense added",
                    Toast.LENGTH_SHORT
            ).show();


            if(refresh!=null)
                refresh.run();



            dismiss();


        }else{


            Toast.makeText(
                    getContext(),
                    "Failed to add expense",
                    Toast.LENGTH_SHORT
            ).show();

        }


    }


}