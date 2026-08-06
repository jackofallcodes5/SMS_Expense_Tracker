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


    private final Transaction transaction;
    private final DatabaseHelper db;
    private final OnDescriptionSavedListener listener;



    private TextView tvDialogDatetime;
    private TextView tvDialogAmount;
    private TextView tvDialogType;
    private TextView tvDialogParty;
    private TextView tvDialogReference;


    private EditText etDialogDescription;


    private Spinner spinnerExpense;
    private TextView tvExpenseLabel;


    private Button btnSave;
    private Button btnClose;



    private List<String> expenseNames = new ArrayList<>();



    public TransactionDialog(
            @NonNull Context context,
            @NonNull Transaction transaction,
            @NonNull DatabaseHelper db,
            OnDescriptionSavedListener listener) {


        super(context);

        this.transaction = transaction;

        this.db = db;

        this.listener = listener;

    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {

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
                R.layout.dialog_transaction
        );



        bindViews();

        populateData();

        setupExpenseSpinner();

        setClickListeners();

    }





    private void bindViews(){


        tvDialogDatetime =
                findViewById(R.id.tvDialogDatetime);


        tvDialogAmount =
                findViewById(R.id.tvDialogAmount);


        tvDialogType =
                findViewById(R.id.tvDialogType);


        tvDialogParty =
                findViewById(R.id.tvDialogParty);


        tvDialogReference =
                findViewById(R.id.tvDialogReference);



        etDialogDescription =
                findViewById(R.id.etDialogDescription);



        spinnerExpense =
                findViewById(R.id.spinnerExpense);



        tvExpenseLabel =
                findViewById(R.id.tvExpenseLabel);



        btnSave =
                findViewById(R.id.btnSave);



        btnClose =
                findViewById(R.id.btnClose);


    }







    private void populateData(){



        tvDialogDatetime.setText(
                transaction.getDatetime()
        );



        tvDialogAmount.setText(
                String.format(
                        "₹ %.2f",
                        transaction.getAmount()
                )
        );



        String type =
                transaction.getType();



        tvDialogType.setText(type);



        if("Credited".equalsIgnoreCase(type)){


            tvDialogType.setTextColor(
                    Color.parseColor("#2E7D32")
            );


        }else if("Debited".equalsIgnoreCase(type)){


            tvDialogType.setTextColor(
                    Color.parseColor("#C62828")
            );


        }



        tvDialogParty.setText(

                TextUtils.isEmpty(
                        transaction.getParty()
                )
                        ?
                        "N/A"
                        :
                        transaction.getParty()

        );



        tvDialogReference.setText(

                TextUtils.isEmpty(
                        transaction.getReference()
                )
                        ?
                        "N/A"
                        :
                        transaction.getReference()

        );



        etDialogDescription.setText(
                transaction.getDescription()
        );


        etDialogDescription.setSelection(
                etDialogDescription.length()
        );


    }







    private void setupExpenseSpinner(){



        // Only debit transactions need expense mapping

        if(!"Debited".equalsIgnoreCase(
                transaction.getType()
        )){


            spinnerExpense.setVisibility(
                    android.view.View.GONE
            );


            tvExpenseLabel.setVisibility(
                    android.view.View.GONE
            );


            return;

        }





        spinnerExpense.setVisibility(
                android.view.View.VISIBLE
        );


        tvExpenseLabel.setVisibility(
                android.view.View.VISIBLE
        );





        expenseNames =
                db.getExpenseNames();




        if(expenseNames.isEmpty()){


            expenseNames.add(
                    "No expense category"
            );


        }




        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        getContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        expenseNames
                );



        spinnerExpense.setAdapter(adapter);




    }









    private void setClickListeners(){


        btnClose.setOnClickListener(
                v -> dismiss()
        );



        btnSave.setOnClickListener(
                v -> saveTransaction()
        );


    }









    private void saveTransaction(){



        String description =
                etDialogDescription
                        .getText()
                        .toString()
                        .trim();





        if(TextUtils.isEmpty(description)){


            Toast.makeText(
                    getContext(),
                    "Description cannot be empty",
                    Toast.LENGTH_SHORT
            ).show();


            return;

        }





        int rows =
                db.updateDescription(
                        transaction.getId(),
                        description
                );



        transaction.setDescription(
                description
        );







        // Save selected expense

        if("Debited".equalsIgnoreCase(
                transaction.getType()
        )
                &&
                !expenseNames.isEmpty()){


            String selected =
                    spinnerExpense
                            .getSelectedItem()
                            .toString();



            if(!selected.equals(
                    "No expense category"
            )){


                int expenseId =
                        getExpenseId(selected);



                if(expenseId != -1){



                    db.updateTransactionExpense(
                            transaction.getId(),
                            expenseId
                    );



                    db.addExpenseSpent(
                            expenseId,
                            transaction.getAmount()
                    );



                    transaction.setExpenseId(
                            expenseId
                    );

                }

            }

        }





        if(rows > 0){


            Toast.makeText(
                    getContext(),
                    "Transaction updated",
                    Toast.LENGTH_SHORT
            ).show();


            if(listener!=null)
                listener.onSaved();


            dismiss();


        }else{


            Toast.makeText(
                    getContext(),
                    "Update failed",
                    Toast.LENGTH_SHORT
            ).show();

        }


    }







    private int getExpenseId(String name){



        List<Expense> list =
                db.getAllExpenses();



        for(Expense e:list){


            if(e.getName()
                    .equals(name)){


                return e.getId();

            }

        }


        return -1;

    }


}