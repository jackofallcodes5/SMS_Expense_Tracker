package com.example.smsexpensetracker;

public class Expense {

    private int id;
    private String name;
    private double monthlyBudget;
    private double spentAmount;
    private String createdDate;


    public Expense() {
    }


    public Expense(String name,
                   double monthlyBudget,
                   double spentAmount,
                   String createdDate) {

        this.name = name;
        this.monthlyBudget = monthlyBudget;
        this.spentAmount = spentAmount;
        this.createdDate = createdDate;
    }


    // ID

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    // Expense Name

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // Monthly Budget

    public double getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(double monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }


    // Amount Spent

    public double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(double spentAmount) {
        this.spentAmount = spentAmount;
    }


    // Created Date

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }


    public double getRemainingAmount() {
        return monthlyBudget - spentAmount;
    }


    /**
     * Returns budget usage percentage
     */
    public int getUsagePercentage() {

        if (monthlyBudget <= 0)
            return 0;

        return (int)((spentAmount / monthlyBudget) * 100);
    }


    @Override
    public String toString() {

        return "Expense{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", budget=" + monthlyBudget +
                ", spent=" + spentAmount +
                '}';
    }
}