package com.example.smsexpensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {


    private static final String TAG = "DatabaseHelper";


    private static final String DATABASE_NAME = "sms_expenses.db";

    private static final int DATABASE_VERSION = 3;



    // ===============================
    // TRANSACTION TABLE
    // ===============================

    public static final String TABLE_TRANSACTIONS = "transactions";


    public static final String COL_ID = "id";
    public static final String COL_DATETIME = "datetime";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_TYPE = "type";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_PARTY = "party";
    public static final String COL_REFERENCE = "reference";
    public static final String COL_SMS_ID = "sms_id";
    public static final String COL_SMS_DATE = "sms_date";

    // New column
    public static final String COL_EXPENSE_ID = "expense_id";




    // ===============================
    // EXPENSE TABLE
    // ===============================


    public static final String TABLE_EXPENSES = "expenses";


    public static final String EXP_ID = "id";
    public static final String EXP_NAME = "name";
    public static final String EXP_BUDGET = "monthly_budget";
    public static final String EXP_SPENT = "spent_amount";
    public static final String EXP_DATE = "created_date";





    private static final String CREATE_TRANSACTIONS =

            "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +

                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                    COL_DATETIME + " TEXT," +

                    COL_AMOUNT + " REAL," +

                    COL_TYPE + " TEXT," +

                    COL_DESCRIPTION + " TEXT," +

                    COL_PARTY + " TEXT," +

                    COL_REFERENCE + " TEXT," +

                    COL_SMS_ID + " TEXT UNIQUE," +

                    COL_SMS_DATE + " INTEGER DEFAULT 0," +

                    COL_EXPENSE_ID + " INTEGER DEFAULT -1" +

                    ");";





    private static final String CREATE_EXPENSES =

            "CREATE TABLE " + TABLE_EXPENSES + " (" +

                    EXP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                    EXP_NAME + " TEXT UNIQUE," +

                    EXP_BUDGET + " REAL," +

                    EXP_SPENT + " REAL DEFAULT 0," +

                    EXP_DATE + " TEXT" +

                    ");";





    public DatabaseHelper(Context context){

        super(context,
                DATABASE_NAME,
                null,
                DATABASE_VERSION);

    }





    @Override
    public void onCreate(SQLiteDatabase db){


        db.execSQL(CREATE_TRANSACTIONS);

        db.execSQL(CREATE_EXPENSES);


        Log.d(TAG,"Database created");

    }





    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion,
                          int newVersion){



        if(oldVersion < 3){


            db.execSQL(
                    "ALTER TABLE "
                            + TABLE_TRANSACTIONS
                            + " ADD COLUMN "
                            + COL_EXPENSE_ID
                            + " INTEGER DEFAULT -1"
            );


            db.execSQL(CREATE_EXPENSES);


        }

    }






    // ==========================================
    // TRANSACTION INSERT
    // ==========================================


    public int insertTransactions(List<Transaction> list){


        SQLiteDatabase db =
                getWritableDatabase();


        int count = 0;



        db.beginTransaction();


        try{


            for(Transaction t:list){


                ContentValues cv =
                        buildTransactionValues(t);



                long id =
                        db.insertWithOnConflict(
                                TABLE_TRANSACTIONS,
                                null,
                                cv,
                                SQLiteDatabase.CONFLICT_IGNORE
                        );



                if(id!=-1)
                    count++;

            }


            db.setTransactionSuccessful();



        }finally{

            db.endTransaction();

        }


        return count;

    }






    public long insertSingleTransaction(Transaction t){


        SQLiteDatabase db =
                getWritableDatabase();



        return db.insertWithOnConflict(
                TABLE_TRANSACTIONS,
                null,
                buildTransactionValues(t),
                SQLiteDatabase.CONFLICT_IGNORE
        );

    }







    private ContentValues buildTransactionValues(Transaction t){


        ContentValues cv =
                new ContentValues();



        cv.put(COL_DATETIME,t.getDatetime());

        cv.put(COL_AMOUNT,t.getAmount());

        cv.put(COL_TYPE,t.getType());

        cv.put(COL_DESCRIPTION,t.getDescription());

        cv.put(COL_PARTY,t.getParty());

        cv.put(COL_REFERENCE,t.getReference());

        cv.put(COL_SMS_ID,t.getSmsId());

        cv.put(COL_SMS_DATE,t.getSmsDate());

        cv.put(
                COL_EXPENSE_ID,
                t.getExpenseId()
        );


        return cv;

    }


    // ==========================================
// READ TRANSACTIONS
// ==========================================


    public List<Transaction> getAllTransactions() {


        List<Transaction> list = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();



        Cursor cursor = db.query(
                TABLE_TRANSACTIONS,
                null,
                null,
                null,
                null,
                null,
                COL_SMS_DATE + " DESC, "
                        + COL_ID + " DESC"
        );



        if(cursor != null && cursor.moveToFirst()){


            do{


                list.add(cursorToTransaction(cursor));


            }while(cursor.moveToNext());


            cursor.close();

        }


        return list;

    }






    private Transaction cursorToTransaction(Cursor c){


        Transaction t = new Transaction();



        t.setId(
                c.getInt(
                        c.getColumnIndexOrThrow(COL_ID)
                )
        );


        t.setDatetime(
                c.getString(
                        c.getColumnIndexOrThrow(COL_DATETIME)
                )
        );


        t.setAmount(
                c.getDouble(
                        c.getColumnIndexOrThrow(COL_AMOUNT)
                )
        );


        t.setType(
                c.getString(
                        c.getColumnIndexOrThrow(COL_TYPE)
                )
        );


        t.setDescription(
                c.getString(
                        c.getColumnIndexOrThrow(COL_DESCRIPTION)
                )
        );


        t.setParty(
                c.getString(
                        c.getColumnIndexOrThrow(COL_PARTY)
                )
        );


        t.setReference(
                c.getString(
                        c.getColumnIndexOrThrow(COL_REFERENCE)
                )
        );


        t.setSmsId(
                c.getString(
                        c.getColumnIndexOrThrow(COL_SMS_ID)
                )
        );


        t.setSmsDate(
                c.getLong(
                        c.getColumnIndexOrThrow(COL_SMS_DATE)
                )
        );



        t.setExpenseId(
                c.getInt(
                        c.getColumnIndexOrThrow(COL_EXPENSE_ID)
                )
        );



        return t;

    }





// ==========================================
// DESCRIPTION UPDATE
// ==========================================


    public int updateDescription(
            int transactionId,
            String description){



        SQLiteDatabase db =
                getWritableDatabase();



        ContentValues cv =
                new ContentValues();



        cv.put(
                COL_DESCRIPTION,
                description
        );



        return db.update(
                TABLE_TRANSACTIONS,
                cv,
                COL_ID+"=?",
                new String[]{
                        String.valueOf(transactionId)
                }
        );

    }





// ==========================================
// EXPENSE INSERT
// ==========================================


    public long insertExpense(Expense expense){


        SQLiteDatabase db =
                getWritableDatabase();



        ContentValues cv =
                new ContentValues();



        cv.put(
                EXP_NAME,
                expense.getName()
        );


        cv.put(
                EXP_BUDGET,
                expense.getMonthlyBudget()
        );


        cv.put(
                EXP_SPENT,
                expense.getSpentAmount()
        );


        cv.put(
                EXP_DATE,
                expense.getCreatedDate()
        );



        return db.insert(
                TABLE_EXPENSES,
                null,
                cv
        );

    }






// ==========================================
// GET ALL EXPENSES
// ==========================================


    public List<Expense> getAllExpenses(){


        List<Expense> list =
                new ArrayList<>();


        SQLiteDatabase db =
                getReadableDatabase();



        Cursor c =
                db.query(
                        TABLE_EXPENSES,
                        null,
                        null,
                        null,
                        null,
                        null,
                        EXP_ID+" DESC"
                );



        if(c!=null && c.moveToFirst()){


            do{


                Expense e =
                        new Expense();



                e.setId(
                        c.getInt(
                                c.getColumnIndexOrThrow(EXP_ID)
                        )
                );


                e.setName(
                        c.getString(
                                c.getColumnIndexOrThrow(EXP_NAME)
                        )
                );


                e.setMonthlyBudget(
                        c.getDouble(
                                c.getColumnIndexOrThrow(EXP_BUDGET)
                        )
                );


                e.setSpentAmount(
                        c.getDouble(
                                c.getColumnIndexOrThrow(EXP_SPENT)
                        )
                );


                e.setCreatedDate(
                        c.getString(
                                c.getColumnIndexOrThrow(EXP_DATE)
                        )
                );



                list.add(e);



            }while(c.moveToNext());


            c.close();

        }



        return list;

    }






// ==========================================
// EXPENSE NAME LIST
// Used by TransactionDialog Spinner
// ==========================================


    public List<String> getExpenseNames(){


        List<String> names =
                new ArrayList<>();


        Cursor c =
                getReadableDatabase()
                        .query(
                                TABLE_EXPENSES,
                                new String[]{EXP_NAME},
                                null,
                                null,
                                null,
                                null,
                                EXP_NAME+" ASC"
                        );



        if(c!=null && c.moveToFirst()){


            do{


                names.add(
                        c.getString(0)
                );


            }while(c.moveToNext());


            c.close();

        }


        return names;

    }






// ==========================================
// ASSIGN EXPENSE TO TRANSACTION
// ==========================================


    public int updateTransactionExpense(
            int transactionId,
            int expenseId){



        SQLiteDatabase db =
                getWritableDatabase();



        ContentValues cv =
                new ContentValues();



        cv.put(
                COL_EXPENSE_ID,
                expenseId
        );



        return db.update(
                TABLE_TRANSACTIONS,
                cv,
                COL_ID+"=?",
                new String[]{
                        String.valueOf(transactionId)
                }
        );

    }







// ==========================================
// ADD SPENT AMOUNT WHEN TRANSACTION ASSIGNED
// ==========================================


    public void addExpenseSpent(
            int expenseId,
            double amount){

        SQLiteDatabase db = getWritableDatabase();

        db.execSQL(
                "UPDATE " + TABLE_EXPENSES +
                " SET " + EXP_SPENT +
                " = " + EXP_SPENT +
                " + ? WHERE " + EXP_ID + "=?",
                new Object[]{ amount, expenseId }
        );
    }


// ==========================================
// SUBTRACT SPENT AMOUNT WHEN TRANSACTION REMOVED
// ==========================================


    public void subtractExpenseSpent(
            int expenseId,
            double amount){

        SQLiteDatabase db = getWritableDatabase();

        db.execSQL(
                "UPDATE " + TABLE_EXPENSES +
                " SET " + EXP_SPENT +
                " = MAX(0, " + EXP_SPENT +
                " - ?) WHERE " + EXP_ID + "=?",
                new Object[]{ amount, expenseId }
        );
    }


// ==========================================
// GET CURRENT EXPENSE_ID FOR A TRANSACTION
// ==========================================


    /** Returns the expense_id currently linked to the given transaction, or -1 if none. */
    public int getExpenseIdForTransaction(int transactionId){

        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(
                TABLE_TRANSACTIONS,
                new String[]{ COL_EXPENSE_ID },
                COL_ID + "=?",
                new String[]{ String.valueOf(transactionId) },
                null, null, null
        );

        int expenseId = -1;

        if(c != null){
            if(c.moveToFirst()){
                expenseId = c.getInt(0);
            }
            c.close();
        }

        return expenseId;
    }


// ==========================================
// CLEAR EXPENSE LINK FROM A TRANSACTION
// ==========================================


    /** Sets expense_id = -1 for the given transaction (unlinks it). */
    public void clearTransactionExpense(int transactionId){

        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(COL_EXPENSE_ID, -1);

        db.update(
                TABLE_TRANSACTIONS,
                cv,
                COL_ID + "=?",
                new String[]{ String.valueOf(transactionId) }
        );
    }


// ==========================================
// DELETE EXPENSE
// ==========================================


    /** Deletes expense record and unlinks any transactions linked to it. */
    public int deleteExpense(Expense expense) {
        if (expense == null) return 0;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_EXPENSE_ID, -1);

            int expenseId = expense.getId();

            if (expenseId > 0) {
                db.update(
                        TABLE_TRANSACTIONS,
                        cv,
                        COL_EXPENSE_ID + "=?",
                        new String[]{ String.valueOf(expenseId) }
                );
            }

            int rows = 0;
            if (expenseId > 0) {
                rows = db.delete(
                        TABLE_EXPENSES,
                        EXP_ID + "=?",
                        new String[]{ String.valueOf(expenseId) }
                );
            }

            // Fallback to name match if id match didn't delete any rows
            if (rows == 0 && expense.getName() != null) {
                rows = db.delete(
                        TABLE_EXPENSES,
                        EXP_NAME + "=?",
                        new String[]{ expense.getName() }
                );
            }

            db.setTransactionSuccessful();
            return rows;

        } finally {
            db.endTransaction();
        }
    }


    public int deleteExpense(int expenseId) {
        Expense e = new Expense();
        e.setId(expenseId);
        return deleteExpense(e);
    }

}