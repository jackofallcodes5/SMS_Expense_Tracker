package com.example.smsexpensetracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {


    private final Context context;
    private List<Expense> expenseList;


    public ExpenseAdapter(Context context,
                          List<Expense> expenseList) {

        this.context = context;
        this.expenseList = expenseList;
    }



    public void updateData(List<Expense> list) {

        this.expenseList = list;
        notifyDataSetChanged();
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {


        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_expense, parent, false);

        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {


        Expense expense = expenseList.get(position);


        holder.tvName.setText(expense.getName());


        holder.tvBudget.setText(
                String.format(
                        "Budget : ₹%.0f",
                        expense.getMonthlyBudget()
                )
        );


        holder.tvSpent.setText(
                String.format(
                        "Spent : ₹%.0f",
                        expense.getSpentAmount()
                )
        );


        holder.tvRemaining.setText(
                String.format(
                        "Remaining : ₹%.0f",
                        expense.getRemainingAmount()
                )
        );


        int progress = expense.getUsagePercentage();


        if(progress > 100)
            progress = 100;


        holder.progressBar.setProgress(progress);


        // Change remaining text if budget exceeded

        if(expense.getRemainingAmount() < 0){

            holder.tvRemaining.setTextColor(
                    android.graphics.Color.RED
            );

        }else{

            holder.tvRemaining.setTextColor(
                    android.graphics.Color.parseColor("#2E7D32")
            );
        }

    }



    @Override
    public int getItemCount() {

        return expenseList == null
                ? 0
                : expenseList.size();
    }




    static class ViewHolder extends RecyclerView.ViewHolder {


        TextView tvName;
        TextView tvBudget;
        TextView tvSpent;
        TextView tvRemaining;
        ProgressBar progressBar;



        public ViewHolder(@NonNull View itemView) {

            super(itemView);


            tvName =
                    itemView.findViewById(R.id.tvExpenseName);

            tvBudget =
                    itemView.findViewById(R.id.tvBudget);

            tvSpent =
                    itemView.findViewById(R.id.tvSpent);

            tvRemaining =
                    itemView.findViewById(R.id.tvRemaining);

            progressBar =
                    itemView.findViewById(R.id.progressExpense);
        }
    }
}