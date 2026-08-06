package com.example.smsexpensetracker;

public class Transaction {

    private int    id;
    private String datetime;
    private double amount;
    private String type;
    private String description;
    private String party;
    private String reference;
    private String smsId;
    private long   smsDate;

    // New field - linked expense category
    private int expenseId = -1;


    public Transaction() {}


    public Transaction(String datetime,
                       double amount,
                       String type,
                       String description,
                       String party,
                       String reference,
                       String smsId,
                       long smsDate) {

        this.datetime    = datetime;
        this.amount      = amount;
        this.type        = type;
        this.description = description;
        this.party       = party;
        this.reference   = reference;
        this.smsId       = smsId;
        this.smsDate     = smsDate;
        this.expenseId   = -1;
    }


    // -------------------------------------------------------
    // Basic Transaction Fields
    // -------------------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }


    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }


    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }


    public String getSmsId() {
        return smsId;
    }

    public void setSmsId(String smsId) {
        this.smsId = smsId;
    }


    public long getSmsDate() {
        return smsDate;
    }

    public void setSmsDate(long smsDate) {
        this.smsDate = smsDate;
    }


    // -------------------------------------------------------
    // Expense Category
    // -------------------------------------------------------

    /**
     * Returns assigned expense category ID.
     * -1 means not assigned yet.
     */
    public int getExpenseId() {
        return expenseId;
    }


    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }


    @Override
    public String toString() {

        return "Transaction{" +
                "id=" + id +
                ", datetime='" + datetime + '\'' +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                ", expenseId=" + expenseId +
                '}';
    }
}