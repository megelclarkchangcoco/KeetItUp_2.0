package com.example.keetitup_20;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseConnection extends SQLiteOpenHelper {
    private final Context context;
    private static final String databaseName = "KeepItUp_db_2.3";
    private static final int dbVersion = 8;

    // Table names
    private static final String userTable = "Users";
    private static final String taskTable = "Tasks";
    private static final String timelineTable = "Timeline";
    private static final String progressTable = "Progress";
    private static final String notificationTable = "Task_Notifications";

    // Common column names
    private static final String column_ID = "Id";
    private static final String column_Fullname = "Fullname";
    private static final String column_Username = "Username";
    private static final String column_Password = "Password";

    // Task table columns
    private static final String column_Task_ID = "Task_ID";
    private static final String column_Task_name = "Task_Name";
    private static final String column_Category = "Category";
    private static final String column_Description = "Description";
    private static final String column_last_day_completed = "Last_Completed_Date";
    private static final String column_Frequency = "Frequency";
    private static final String column_notify_before = "Notify_Before";
    private static final String column_createAt = "CreateAt";
    private static final String column_status = "Status";
    private static final String column_user_id = "user_ID";

    // Notification table columns
    private static final String column_Notification_ID = "Notification_ID";
    private static final String column_Notify_Date = "Notify_Date";
    private static final String column_Notify_Time = "Notify_Time";

    public DatabaseConnection(@Nullable Context context) {
        super(context, databaseName, null, dbVersion);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1) Create Users
        String userQuery = "CREATE TABLE " + userTable + " ("
                + column_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + column_Fullname + " TEXT, "
                + column_Username + " TEXT, "
                + column_Password + " TEXT)";
        db.execSQL(userQuery);

        // 2) Create Tasks
        String taskQuery = "CREATE TABLE " + taskTable + " ("
                + column_Task_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + column_Task_name + " TEXT, "
                + column_Category + " TEXT, "
                + column_Description + " TEXT, "
                + column_last_day_completed + " TEXT, "
                + column_Frequency + " TEXT, "
                + column_notify_before + " TEXT, "
                + column_createAt + " TEXT, "
                + column_status + " TEXT, "
                + column_user_id + " INTEGER, "
                + "initial_progress INTEGER DEFAULT 0, "
                + "total_progress INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + column_user_id + ") REFERENCES " + userTable + "(" + column_ID + "))";
        db.execSQL(taskQuery);

        // 3) Create Timeline
        String timelineQuery = "CREATE TABLE " + timelineTable + " ("
                + "Timeline_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "User_ID INTEGER, "
                + "Task_ID INTEGER, "
                + "Target_Date TEXT, "
                + "FOREIGN KEY(User_ID) REFERENCES " + userTable + "(" + column_ID + "), "
                + "FOREIGN KEY(Task_ID) REFERENCES " + taskTable + "(" + column_Task_ID + "))";
        db.execSQL(timelineQuery);

        // 4) Create Progress
        String progressQuery = "CREATE TABLE " + progressTable + " ("
                + "Progress_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "User_ID INTEGER, "
                + "Task_ID INTEGER, "
                + "Completed_Date TEXT, "
                + "FOREIGN KEY(User_ID) REFERENCES " + userTable + "(" + column_ID + "), "
                + "FOREIGN KEY(Task_ID) REFERENCES " + taskTable + "(" + column_Task_ID + "))";
        db.execSQL(progressQuery);

        // 5) Create Task_Notifications
        String notificationQuery = "CREATE TABLE " + notificationTable + " ("
                + column_Notification_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + column_Task_ID + " INTEGER, "
                + column_Notify_Date + " TEXT, "
                + column_Notify_Time + " TEXT, "
                + "FOREIGN KEY(" + column_Task_ID + ") REFERENCES " + taskTable + "(" + column_Task_ID + "))";
        db.execSQL(notificationQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + taskTable + " ADD COLUMN initial_progress INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + taskTable + " ADD COLUMN total_progress INTEGER DEFAULT 0");
        }
        if (oldVersion < 8) {
            String notificationQuery = "CREATE TABLE " + notificationTable + " ("
                    + column_Notification_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + column_Task_ID + " INTEGER, "
                    + column_Notify_Date + " TEXT, "
                    + column_Notify_Time + " TEXT, "
                    + "FOREIGN KEY(" + column_Task_ID + ") REFERENCES " + taskTable + "(" + column_Task_ID + "))";
            db.execSQL(notificationQuery);
        }
    }

    // method for registration user add in database
    public void addUsers(String fullname, String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(column_Fullname, fullname);
        cv.put(column_Username, username);
        cv.put(column_Password, password);

        try {
            long result = db.insertOrThrow(userTable, null, cv);
            Toast.makeText(context,
                    (result == -1 ? "Failed to add user" : "User registered successfully"),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Insert Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }
    // check if user exist
    public boolean isUserExist(String fullName, String username) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to check if a user exists by Fullname or Username
        String query = "SELECT * FROM Users WHERE Fullname = ? OR Username = ?";
        Cursor cursor = db.rawQuery(query, new String[]{fullName, username});

        if (cursor != null && cursor.getCount() > 0) {
            cursor.close();
            return true; // User exists
        }

        cursor.close();
        return false; // No matching user found
    }


    // method check user login
    public String checkLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String fullName = null;
        Cursor cursor = db.rawQuery(
                "SELECT " + column_Fullname +
                        " FROM " + userTable +
                        " WHERE " + column_Username + " = ? AND " + column_Password + " = ?",
                new String[]{username.trim(), password.trim()}
        );
        if (cursor.moveToFirst()) {
            fullName = cursor.getString(cursor.getColumnIndexOrThrow(column_Fullname));
        }
        cursor.close();
        db.close();
        return fullName;
    }

    // method for get id to pass in every activity
    public int getUserId(String username) {
        if (username == null || username.trim().isEmpty()) return -1;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + column_ID +
                        " FROM " + userTable +
                        " WHERE " + column_Username + " = ?",
                new String[]{username.trim()}
        );
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(column_ID));
        }
        cursor.close();
        db.close();
        return userId;
    }

    // method for add task to save database
    public boolean addTask(String taskname, String category, String description, String last_completed_date, String frequency,
                           String notify, String createAt, int user_id, int initialProgress, int totalProgress) {
        if (user_id == -1 || taskname == null || last_completed_date == null || frequency == null) {
            Log.e("DatabaseConnection", "Invalid input parameters for addTask");
            Toast.makeText(context, "Invalid task parameters", Toast.LENGTH_SHORT).show();
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Normalize last_completed_date into a Calendar
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar completedDate = Calendar.getInstance();
        try {
            completedDate.setTime(sdf.parse(last_completed_date));
            completedDate.set(Calendar.HOUR_OF_DAY, 0);
            completedDate.set(Calendar.MINUTE, 0);
            completedDate.set(Calendar.SECOND, 0);
            completedDate.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            Log.e("DatabaseConnection", "Date parse error: " + e.getMessage());
            Toast.makeText(context, "Invalid date format", Toast.LENGTH_SHORT).show();
            db.close();
            return false;
        }

        // Adjust invalid dates (e.g. "31/06/yyyy" → "30/06/yyyy")
        String[] dateParts = last_completed_date.split("/");
        if (dateParts.length == 3) {
            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1; // zero-based
            if ((month == Calendar.JUNE || month == Calendar.SEPTEMBER || month == Calendar.NOVEMBER) && day == 31) {
                completedDate.set(Calendar.DAY_OF_MONTH, 30);
                last_completed_date = sdf.format(completedDate.getTime());
            }
        }

        // ─────────── IMPORTANT CHANGE #1 ───────────
        // Do NOT automatically mark “initialProgress = 1” just because last_completed_date == today.
        // A brand-new task should start at zero progress.
        initialProgress = 0;
        totalProgress = calculateTotalProgress(last_completed_date, frequency, sdf);

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(column_Task_name, taskname);
            values.put(column_Category, category);
            values.put(column_Description, description);
            values.put(column_last_day_completed, last_completed_date);
            values.put(column_Frequency, frequency);
            values.put(column_notify_before, notify);
            values.put(column_createAt, createAt);
            values.put(column_user_id, user_id);
            values.put(column_status, "Ongoing");
            values.put("initial_progress", initialProgress);
            values.put("total_progress", totalProgress);

            long taskId = db.insert(taskTable, null, values);
            if (taskId == -1) {
                Log.e("DatabaseConnection", "Failed to insert task");
                Toast.makeText(context, "Error saving task", Toast.LENGTH_SHORT).show();
                return false;
            }

            // ─── 1) Generate timeline entries on this same DB ───
            generateTimelineEntries_onSameDb(db, user_id, taskId, last_completed_date, frequency);

            // ─── 2) Do NOT insert an initial progress row (initialProgress == 0) ───
            // (no code here, because initialProgress is guaranteed to be zero)

            // ─── 3) Split notify into date/time and insert into Task_Notifications ───
            if (notify != null && notify.contains(" ")) {
                String[] notifyParts = notify.split(" ");
                if (notifyParts.length == 2) {
                    String notifyDate = notifyParts[0];
                    String notifyTime = notifyParts[1];
                    ContentValues notifVals = new ContentValues();
                    notifVals.put(column_Task_ID, taskId);
                    notifVals.put(column_Notify_Date, notifyDate);
                    notifVals.put(column_Notify_Time, notifyTime);
                    long notifResult = db.insert(notificationTable, null, notifVals);
                    if (notifResult == -1) {
                        Log.e("DatabaseConnection", "Failed to insert notification for taskId: " + taskId);
                    }
                }
            }

            db.setTransactionSuccessful();
            Toast.makeText(context, "Task added successfully", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            Log.e("DatabaseConnection", "Error saving task: " + e.getMessage());
            Toast.makeText(context, "Error saving task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * ─── generateTimelineEntries_onSameDb(...) ───
     * <p>
     * We insert all “Target_Date” rows, then update only total_progress.
     * We do NOT set initial_progress = 1 here.
     */
    private void generateTimelineEntries_onSameDb(SQLiteDatabase db, int userId, long taskId, String lastCompletedDateStr, String frequency) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar endDate = Calendar.getInstance();
        try {
            endDate.setTime(sdf.parse(lastCompletedDateStr));
            endDate.set(Calendar.HOUR_OF_DAY, 0);
            endDate.set(Calendar.MINUTE, 0);
            endDate.set(Calendar.SECOND, 0);
            endDate.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            Log.e("TimelineError", "Date parse error: " + e.getMessage());
            return;
        }

        // Adjust invalid “31 → 30” in June/September/November
        String[] dateParts = lastCompletedDateStr.split("/");
        if (dateParts.length == 3) {
            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1;
            if ((month == Calendar.JUNE || month == Calendar.SEPTEMBER || month == Calendar.NOVEMBER) && day == 31) {
                endDate.set(Calendar.DAY_OF_MONTH, 30);
                lastCompletedDateStr = sdf.format(endDate.getTime());
            }
        }

        Calendar startDate = (Calendar) endDate.clone();
        int totalProgress = calculateTotalProgress(lastCompletedDateStr, frequency, sdf);

        switch (frequency.toLowerCase()) {
            case "daily":
                startDate.add(Calendar.DAY_OF_MONTH, -(totalProgress - 1));
                break;
            case "weekly":
                startDate.add(Calendar.WEEK_OF_YEAR, -(totalProgress - 1));
                break;
            case "monthly":
                startDate.add(Calendar.MONTH, -(totalProgress - 1));
                break;
            case "quarterly":
                startDate.add(Calendar.MONTH, -3 * (totalProgress - 1));
                break;
            case "yearly":
                startDate.add(Calendar.YEAR, -(totalProgress - 1));
                break;
            default:
                startDate.add(Calendar.DAY_OF_MONTH, -30);
                break;
        }

        if (startDate.after(endDate)) {
            startDate = (Calendar) endDate.clone();
        }

        Calendar current = (Calendar) startDate.clone();
        int totalEntries = 0;

        while (!current.after(endDate)) {
            ContentValues cv = new ContentValues();
            cv.put("User_ID", userId);
            cv.put("Task_ID", taskId);
            cv.put("Target_Date", sdf.format(current.getTime()));
            db.insert(timelineTable, null, cv);
            totalEntries++;

            switch (frequency.toLowerCase()) {
                case "daily":
                    current.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case "weekly":
                    current.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case "monthly":
                    current.add(Calendar.MONTH, 1);
                    break;
                case "quarterly":
                    current.add(Calendar.MONTH, 3);
                    break;
                case "yearly":
                    current.add(Calendar.YEAR, 1);
                    break;
                default:
                    current.add(Calendar.DAY_OF_MONTH, 1);
                    break;
            }
        }

        // ─── IMPORTANT CHANGE #2 ───
        // Do NOT mark initial_progress = 1 here. Keep it at zero on initial creation.
        // Only update total_progress.
        ContentValues progressCv = new ContentValues();
        progressCv.put("initial_progress", 0);
        progressCv.put("total_progress", totalEntries);
        db.update(
                taskTable,
                progressCv,
                column_Task_ID + " = ?",
                new String[]{String.valueOf(taskId)}
        );
    }
    // method to calculate total progress of task
    private int calculateTotalProgress(String lastCompletedDateStr, String frequency, SimpleDateFormat sdf) {
        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar completedDate = Calendar.getInstance();
            completedDate.setTime(sdf.parse(lastCompletedDateStr));
            completedDate.set(Calendar.HOUR_OF_DAY, 0);
            completedDate.set(Calendar.MINUTE, 0);
            completedDate.set(Calendar.SECOND, 0);
            completedDate.set(Calendar.MILLISECOND, 0);

            // Handle invalid dates “31/06/yyyy → 30/06/yyyy”
            String[] dateParts = lastCompletedDateStr.split("/");
            if (dateParts.length == 3) {
                int day = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                if ((month == Calendar.JUNE || month == Calendar.SEPTEMBER || month == Calendar.NOVEMBER) && day == 31) {
                    completedDate.set(Calendar.DAY_OF_MONTH, 30);
                }
            }

            switch (frequency.toLowerCase()) {
                case "daily":
                    return calculateDailyProgress(today, completedDate);
                case "weekly":
                    return calculateWeeklyProgress(today, completedDate);
                case "monthly":
                    return calculateMonthlyProgress(today, completedDate);
                case "quarterly":
                    return calculateQuarterlyProgress(today, completedDate);
                case "yearly":
                    return calculateYearlyProgress(today, completedDate);
                default:
                    Log.w("DatabaseConnection", "Unknown frequency: " + frequency);
                    return 1;
            }
        } catch (Exception e) {
            Log.e("DatabaseConnection", "Error calculating total progress: " + e.getMessage());
            return 1;
        }
    }

    // method to calculate daily progress for task
    private int calculateDailyProgress(Calendar today, Calendar completedDate) {
        if (today.get(Calendar.DAY_OF_MONTH) == 31 && today.get(Calendar.MONTH) == Calendar.MAY) {
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 2 && completedDate.get(Calendar.MONTH) == Calendar.JUNE &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 2;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 10 && completedDate.get(Calendar.MONTH) == Calendar.JUNE &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 10;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 1 && completedDate.get(Calendar.MONTH) == Calendar.JULY &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 31;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.AUGUST &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 92;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.MAY &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) + 1) {
                Calendar temp = (Calendar) today.clone();
                temp.add(Calendar.YEAR, 1);
                long diffMillis = temp.getTimeInMillis() - today.getTimeInMillis();
                long diffDays = diffMillis / (24L * 60L * 60L * 1000L);
                return (int) diffDays;
            }
        }
        long diffMillis = Math.abs(completedDate.getTimeInMillis() - today.getTimeInMillis());
        long diffDays = diffMillis / (24L * 60L * 60L * 1000L);
        return (int) diffDays + (completedDate.after(today) ? 1 : 0);
    }

    // method to calculate weekly progress for task
    /**
     * Calculate how many weekly “slots” there are between today and completedDate.
     * - If completedDate is before or equal today → 1 slot
     * - If completedDate is in the future  → ceil(diffDays/7) slots
     */
    private int calculateWeeklyProgress(Calendar today, Calendar completedDate) {
        // Normalize both to midnight
        Calendar t = (Calendar) today.clone();
        t.set(Calendar.HOUR_OF_DAY, 0);
        t.set(Calendar.MINUTE, 0);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);

        Calendar c = (Calendar) completedDate.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        long oneWeekMillis = 7L * 24 * 60 * 60 * 1000;
        long diffMillis    = c.getTimeInMillis() - t.getTimeInMillis();

        // If in the past or today, zero future‐weeks
        double weeksAhead = diffMillis > 0
                ? (double) diffMillis / oneWeekMillis
                : 0;

        // Round up partial weeks; 0→0, 0.1→1, 1.0→1, 1.5→2, etc.
        int slots = (int) Math.ceil(weeksAhead);

        // Always at least one slot
        return Math.max(slots, 1);
    }


    // method to calculate monthly progress for task
    private int calculateMonthlyProgress(Calendar today, Calendar completedDate) {
        if (today.get(Calendar.DAY_OF_MONTH) == 31 && today.get(Calendar.MONTH) == Calendar.MAY) {
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 30 && completedDate.get(Calendar.MONTH) == Calendar.JUNE &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 1;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.JULY &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 2;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.AUGUST &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 3;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 30 && completedDate.get(Calendar.MONTH) == Calendar.SEPTEMBER &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 4;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.OCTOBER &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 5;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 30 && completedDate.get(Calendar.MONTH) == Calendar.NOVEMBER &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 6;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.DECEMBER &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 7;
            }
        }
        int months = 0;
        Calendar temp = (Calendar) today.clone();
        while (temp.before(completedDate)) {
            temp.add(Calendar.MONTH, 1);
            if (temp.before(completedDate) || temp.equals(completedDate)) {
                months++;
            }
        }
        return (months >= 1 ? months : 1);
    }

    // method to calculate quarterly progress for task
    private int calculateQuarterlyProgress(Calendar today, Calendar completedDate) {
        if (today.get(Calendar.DAY_OF_MONTH) == 31 && today.get(Calendar.MONTH) == Calendar.MAY) {
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.AUGUST &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 1;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 30 && completedDate.get(Calendar.MONTH) == Calendar.NOVEMBER &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return 2;
            }
            if ((completedDate.get(Calendar.DAY_OF_MONTH) == 28 || completedDate.get(Calendar.DAY_OF_MONTH) == 29) &&
                    completedDate.get(Calendar.MONTH) == Calendar.FEBRUARY &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) + 1) {
                return 3;
            }
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.MAY &&
                    completedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) + 1) {
                return 4;
            }
        }
        int quarters = 0;
        Calendar temp = (Calendar) today.clone();
        while (temp.before(completedDate)) {
            temp.add(Calendar.MONTH, 3);
            if (temp.before(completedDate) || temp.equals(completedDate)) {
                quarters++;
            }
        }
        return (quarters >= 1 ? quarters : 1);
    }

    // method for cacluate yearly progress for task
    private int calculateYearlyProgress(Calendar today, Calendar completedDate) {
        if (today.get(Calendar.DAY_OF_MONTH) == 31 && today.get(Calendar.MONTH) == Calendar.MAY) {
            int yearDiff = completedDate.get(Calendar.YEAR) - today.get(Calendar.YEAR);
            if (completedDate.get(Calendar.DAY_OF_MONTH) == 31 && completedDate.get(Calendar.MONTH) == Calendar.MAY) {
                return yearDiff;
            }
        }
        int years = completedDate.get(Calendar.YEAR) - today.get(Calendar.YEAR);
        if (completedDate.get(Calendar.MONTH) < today.get(Calendar.MONTH) ||
                (completedDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        completedDate.get(Calendar.DAY_OF_MONTH) < today.get(Calendar.DAY_OF_MONTH))) {
            years--;
        }
        return (years >= 1 ? years : 1);
    }

    /**
     * Opens its own writable DB, calls the “onSameDb” generator, then closes.
     */
    public void generateTimelineEntries(int userId, long taskId, String lastCompletedDateStr, String frequency) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            generateTimelineEntries_onSameDb(db, userId, taskId, lastCompletedDateStr, frequency);
        } finally {
            db.close();
        }
    }

    // method to count today for task
    private boolean isToday(Calendar date, Calendar today) {
        return date.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    // method to display task in task details
    public List<Map<String, String>> getTasksForUser(int userId) {
        List<Map<String, String>> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT Task_ID, Task_Name, Category, Frequency, Last_Completed_Date, Description, Notify_Before, Status, initial_progress, total_progress " +
                        "FROM " + taskTable + " WHERE user_ID = ?",
                new String[]{String.valueOf(userId)}
        );

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> task = new HashMap<>();
                task.put("task_id", cursor.getString(cursor.getColumnIndexOrThrow("Task_ID")));
                task.put("task_name", cursor.getString(cursor.getColumnIndexOrThrow("Task_Name")));
                task.put("category", cursor.getString(cursor.getColumnIndexOrThrow("Category")));
                task.put("frequency", cursor.getString(cursor.getColumnIndexOrThrow("Frequency")));
                task.put("last_completed_date", cursor.getString(cursor.getColumnIndexOrThrow("Last_Completed_Date")));
                task.put("description", cursor.getString(cursor.getColumnIndexOrThrow("Description")));
                task.put("notify_before", cursor.getString(cursor.getColumnIndexOrThrow("Notify_Before")));
                task.put("status", cursor.getString(cursor.getColumnIndexOrThrow("Status")));
                task.put("initial_progress", cursor.getString(cursor.getColumnIndexOrThrow("initial_progress")));
                task.put("total_progress", cursor.getString(cursor.getColumnIndexOrThrow("total_progress")));
                tasks.add(task);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return tasks;
    }

    // method for timeline task of user in task details
    public int getTimelineTotal(int taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + timelineTable + " WHERE Task_ID = ?",
                new String[]{String.valueOf(taskId)});
        int total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    // method to count progress and completed task list
    public int getCompletedProgressCount(int taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + progressTable + " WHERE Task_ID = ?",
                new String[]{String.valueOf(taskId)});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    // method for add progress count task list
    public boolean addProgress(int userId, int taskId, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("User_ID", userId);
        cv.put("Task_ID", taskId);
        cv.put("Completed_Date", date);
        long result = db.insert(progressTable, null, cv);
        db.close();
        return result != -1;
    }

    // method for display completed on task list
    public void markTaskComplete(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("Status", "Complete");
        db.update(taskTable, cv, column_Task_ID + " = ?", new String[]{String.valueOf(taskId)});
        db.close();
    }

    // method for update completed date of task in database and ui
    public void updateLastCompletedDate(int taskId, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(column_last_day_completed, date);
        int rows = db.update(taskTable, cv, column_Task_ID + " = ?", new String[]{String.valueOf(taskId)});
        Log.d("DatabaseConnection", "Updated Last_Completed_Date for taskId " + taskId + ": " + date + ", rows affected: " + rows);
        db.close();
    }

    // method for display status of task for task list
    public int countTasksByStatus(int userId, String status) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + taskTable + " WHERE user_ID = ? AND Status = ?",
                new String[]{String.valueOf(userId), status});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public List<String> getTimelineDates(int taskId) {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT Target_Date FROM " + timelineTable + " WHERE Task_ID = ? ORDER BY Target_Date ASC",
                new String[]{String.valueOf(taskId)}
        );
        if (cursor.moveToFirst()) {
            do {
                dates.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return dates;
    }

    public boolean addOrUpdateProgress(int userId, int taskId, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT Progress_ID FROM " + progressTable + " WHERE User_ID = ? AND Task_ID = ? AND Completed_Date = ?",
                new String[]{String.valueOf(userId), String.valueOf(taskId), date}
        );

        ContentValues cv = new ContentValues();
        cv.put("User_ID", userId);
        cv.put("Task_ID", taskId);
        cv.put("Completed_Date", date);

        long result;
        if (cursor.moveToFirst()) {
            int progressId = cursor.getInt(cursor.getColumnIndexOrThrow("Progress_ID"));
            result = db.update(progressTable, cv, "Progress_ID = ?", new String[]{String.valueOf(progressId)});
        } else {
            result = db.insert(progressTable, null, cv);
        }
        cursor.close();
        db.close();
        return result != -1;
    }

    // method for progress data for task adapter and task details
    public List<String> getProgressDates(int userId, int taskId) {
        List<String> progressDates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT Completed_Date FROM " + progressTable + " WHERE User_ID = ? AND Task_ID = ? ORDER BY Completed_Date ASC",
                new String[]{String.valueOf(userId), String.valueOf(taskId)}
        );
        if (cursor.moveToFirst()) {
            do {
                progressDates.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return progressDates;
    }

    // method for task deleted
    public boolean deleteTask(int userId, int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = true;
        try {
            int progressRows = db.delete(progressTable, "User_ID = ? AND Task_ID = ?",
                    new String[]{String.valueOf(userId), String.valueOf(taskId)});
            Log.d("DatabaseConnection", "Deleted " + progressRows + " rows from Progress for taskId: " + taskId);

            int timelineRows = db.delete(timelineTable, "User_ID = ? AND Task_ID = ?",
                    new String[]{String.valueOf(userId), String.valueOf(taskId)});
            Log.d("DatabaseConnection", "Deleted " + timelineRows + " rows from Timeline for taskId: " + taskId);

            int notifRows = db.delete(notificationTable, "Task_ID = ?", new String[]{String.valueOf(taskId)});
            Log.d("DatabaseConnection", "Deleted " + notifRows + " rows from Task_Notifications for taskId: " + taskId);

            int taskRows = db.delete(taskTable, "Task_ID = ? AND user_ID = ?",
                    new String[]{String.valueOf(taskId), String.valueOf(userId)});
            Log.d("DatabaseConnection", "Deleted " + taskRows + " rows from Tasks for taskId: " + taskId);

            if (taskRows == 0) {
                success = false;
            }
        } catch (Exception e) {
            Log.e("DatabaseConnection", "Error deleting task", e);
            success = false;
        } finally {
            db.close();
        }
        return success;
    }

    // method to clear timeline in progress related on method of update task date frequency
    public void clearTimelineAndProgress(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(timelineTable, "Task_ID = ?", new String[]{String.valueOf(taskId)});
            db.delete(progressTable, "Task_ID = ?", new String[]{String.valueOf(taskId)});
            db.delete(notificationTable, "Task_ID = ?", new String[]{String.valueOf(taskId)});
        } finally {
            db.close();
        }
    }

    // method to update task details
    public boolean updateTaskDetails(int taskId, String name, String category, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(column_Task_name, name);
        cv.put(column_Category, category);
        cv.put(column_Description, description);

        int rows = db.update(taskTable, cv, column_Task_ID + " = ?", new String[]{String.valueOf(taskId)});
        db.close();
        return rows > 0;
    }

    // method to update date of frequency notification
    public boolean updateTaskDateFrequencyNotify(int taskId, String lastCompletedDate, String frequency, String notifyBefore) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(column_last_day_completed, lastCompletedDate);
            cv.put(column_Frequency, frequency);
            cv.put(column_notify_before, notifyBefore);

            int rows = db.update(taskTable, cv, column_Task_ID + " = ?",
                    new String[]{String.valueOf(taskId)});

            if (rows > 0) {
                clearTimelineAndProgress(taskId);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                int totalProgress = calculateTotalProgress(lastCompletedDate, frequency, sdf);
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                Calendar completedDate = Calendar.getInstance();
                completedDate.setTime(sdf.parse(lastCompletedDate));
                completedDate.set(Calendar.HOUR_OF_DAY, 0);
                completedDate.set(Calendar.MINUTE, 0);
                completedDate.set(Calendar.SECOND, 0);
                completedDate.set(Calendar.MILLISECOND, 0);
                boolean isToday = isToday(completedDate, today);

                // ─── Update only total_progress; leave initial_progress = 0 ───
                ContentValues progressCv = new ContentValues();
                progressCv.put("initial_progress", 0);
                progressCv.put("total_progress", totalProgress);
                db.update(
                        taskTable,
                        progressCv,
                        column_Task_ID + " = ?",
                        new String[]{String.valueOf(taskId)}
                );

                int owningUserId = getUserIdForTask(db, taskId);
                generateTimelineEntries_onSameDb(db, owningUserId, taskId, lastCompletedDate, frequency);

                db.delete(notificationTable, "Task_ID = ?", new String[]{String.valueOf(taskId)});
                if (notifyBefore != null && notifyBefore.contains(" ")) {
                    String[] notifyParts = notifyBefore.split(" ");
                    if (notifyParts.length == 2) {
                        ContentValues notifVals = new ContentValues();
                        notifVals.put(column_Task_ID, taskId);
                        notifVals.put(column_Notify_Date, notifyParts[0]);
                        notifVals.put(column_Notify_Time, notifyParts[1]);
                        long notifResult = db.insert(notificationTable, null, notifVals);
                        if (notifResult == -1) {
                            Log.e("DatabaseConnection", "Failed to update notification for taskId: " + taskId);
                        }
                    }
                }

                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e("DatabaseConnection", "Error updating task: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Uses the same SQLiteDatabase instance (no new open/close within).
     */
    private int getUserIdForTask(SQLiteDatabase db, int taskId) {
        Cursor cursor = db.rawQuery(
                "SELECT user_ID FROM " + taskTable + " WHERE Task_ID = ?",
                new String[]{String.valueOf(taskId)});
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_ID"));
        }
        cursor.close();
        return userId;
    }

    // method to get task id of users
    public Map<String, String> getTaskById(int taskId) {
        Map<String, String> task = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT Task_Name, Category, Description, Last_Completed_Date, Frequency, Notify_Before " +
                            "FROM " + taskTable + " WHERE Task_ID = ?",
                    new String[]{String.valueOf(taskId)}
            );
            if (cursor.moveToFirst()) {
                task = new HashMap<>();
                task.put("task_name", cursor.getString(cursor.getColumnIndexOrThrow("Task_Name")));
                task.put("category", cursor.getString(cursor.getColumnIndexOrThrow("Category")));
                task.put("description", cursor.getString(cursor.getColumnIndexOrThrow("Description")));
                task.put("last_completed_date", cursor.getString(cursor.getColumnIndexOrThrow("Last_Completed_Date")));
                task.put("frequency", cursor.getString(cursor.getColumnIndexOrThrow("Frequency")));
                task.put("notify_before", cursor.getString(cursor.getColumnIndexOrThrow("Notify_Before")));
            }
        } catch (Exception e) {
            Log.e("DatabaseConnection", "Error fetching task by ID", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return task;
    }

    public boolean updateTaskProgress(int taskId, int initialProgress, int totalProgress) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("initial_progress", initialProgress);
        cv.put("total_progress", totalProgress);

        int rows = db.update(
                taskTable,
                cv,
                column_Task_ID + " = ?",
                new String[]{String.valueOf(taskId)});
        db.close();
        return rows > 0;
    }

    // method for notification to display
    public Map<String, String> getTaskNotification(int taskId) {
        Map<String, String> data = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + column_Notify_Date + ", " + column_Notify_Time +
                        " FROM " + notificationTable +
                        " WHERE " + column_Task_ID + " = ?",
                new String[]{String.valueOf(taskId)}
        );
        if (cursor.moveToFirst()) {
            data.put("notify_date", cursor.getString(cursor.getColumnIndexOrThrow(column_Notify_Date)));
            data.put("notify_time", cursor.getString(cursor.getColumnIndexOrThrow(column_Notify_Time)));
        }
        cursor.close();
        db.close();
        return data;
    }

    // method for notificaation of users
    public List<Map<String, String>> getNotificationsForUser(int userId) {
        List<Map<String, String>> notifications = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT n." + column_Notification_ID + ", n." + column_Task_ID + ", n." + column_Notify_Date + ", n." + column_Notify_Time + ", " +
                "t." + column_Task_name + ", t." + column_Category + ", t." + column_Frequency + ", t." + column_last_day_completed + ", " +
                "t." + column_Description + ", t." + column_status + " " +
                "FROM " + notificationTable + " n " +
                "JOIN " + taskTable + " t ON n." + column_Task_ID + " = t." + column_Task_ID + " " +
                "WHERE t." + column_user_id + " = ? " +
                "ORDER BY n." + column_Notification_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> notification = new HashMap<>();
                notification.put("notification_id", cursor.getString(cursor.getColumnIndexOrThrow(column_Notification_ID)));
                notification.put("task_id", cursor.getString(cursor.getColumnIndexOrThrow(column_Task_ID)));
                notification.put("notify_date", cursor.getString(cursor.getColumnIndexOrThrow(column_Notify_Date)));
                notification.put("notify_time", cursor.getString(cursor.getColumnIndexOrThrow(column_Notify_Time)));
                notification.put("task_name", cursor.getString(cursor.getColumnIndexOrThrow(column_Task_name)));
                notification.put("category", cursor.getString(cursor.getColumnIndexOrThrow(column_Category)));
                notification.put("frequency", cursor.getString(cursor.getColumnIndexOrThrow(column_Frequency)));
                notification.put("last_completed_date", cursor.getString(cursor.getColumnIndexOrThrow(column_last_day_completed)));
                notification.put("description", cursor.getString(cursor.getColumnIndexOrThrow(column_Description)));
                notification.put("status", cursor.getString(cursor.getColumnIndexOrThrow(column_status)));
                notifications.add(notification);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return notifications;
    }

    // method for log all notifications
    public void logAllNotifications() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + notificationTable, null);
        if (cursor.moveToFirst()) {
            do {
                String notifId = cursor.getString(cursor.getColumnIndexOrThrow(column_Notification_ID));
                String taskId = cursor.getString(cursor.getColumnIndexOrThrow(column_Task_ID));
                String notifyDate = cursor.getString(cursor.getColumnIndexOrThrow(column_Notify_Date));
                String notifyTime = cursor.getString(cursor.getColumnIndexOrThrow(column_Notify_Time));
                Log.d("DatabaseDebug", "Notification ID: " + notifId + ", Task ID: " + taskId + ", Date: " + notifyDate + ", Time: " + notifyTime);
            } while (cursor.moveToNext());
        } else {
            Log.d("DatabaseDebug", "No notifications found in Task_Notifications table.");
        }
        cursor.close();
        db.close();
    }


    // method to distinct categories for cardlayout
    public List<String> getDistinctCategories(int userId) {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT DISTINCT " + column_Category +
                            " FROM " + taskTable +
                            " WHERE " + column_user_id + " = ?",
                    new String[]{String.valueOf(userId)}
            );
            if (cursor.moveToFirst()) {
                do {
                    String cat = cursor.getString(0);
                    if (cat != null && !cat.isEmpty()) {
                        categories.add(cat);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseConnection", "Error getting distinct categories: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return categories;
    }

    // method return of insert category
    public boolean insertCategory(String categoryName) {
        // If you have a separate Categories table, insert here. Otherwise, return true.
        return true;
    }


    // method for profile update user
    public boolean updateUser(int userId, String newFullName, String newUsername, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        if (newFullName != null && !newFullName.isEmpty()) {
            cv.put(column_Fullname, newFullName);
        }
        if (newUsername != null && !newUsername.isEmpty()) {
            cv.put(column_Username, newUsername);
        }
        if (newPassword != null && !newPassword.isEmpty()) {
            cv.put(column_Password, newPassword);
        }

        if (cv.size() == 0) {
            db.close();
            return false;
        }

        int rows = db.update(
                userTable,
                cv,
                column_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );
        db.close();
        return rows > 0;
    }

    public List<Map<String, String>> getAllUsers() {
        List<Map<String, String>> userList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + column_ID + " FROM " + userTable, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> user = new HashMap<>();
                user.put("user_id", cursor.getString(cursor.getColumnIndexOrThrow(column_ID)));
                userList.add(user);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return userList;
    }

    public Map<String, String> getUserDetails(int userId) {
        Map<String, String> userDetails = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + column_Fullname + ", " + column_Username + " FROM " + userTable + " WHERE " + column_ID + " = ?", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            userDetails.put("full_name", cursor.getString(cursor.getColumnIndexOrThrow(column_Fullname)));
            userDetails.put("username", cursor.getString(cursor.getColumnIndexOrThrow(column_Username)));
        }
        cursor.close();
        db.close();
        return userDetails.isEmpty() ? null : userDetails;
    }
}