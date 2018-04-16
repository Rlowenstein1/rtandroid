package com.example.rlowe.ramblintreks;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ScheduleActivity extends AppCompatActivity implements
        WeekView.EventClickListener,
        MonthLoader.MonthChangeListener,
        WeekView.EventLongPressListener,
        EasyPermissions.PermissionCallbacks {
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static ArrayList<Event> week;
    private static final String APP_NAME = "schedulintreks";

    GoogleAccountCredential credentials;
    WeekView weekView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.schedule_activity);

        weekView = (WeekView) findViewById(R.id.weekView);
        weekView.setOnEventClickListener(this);
        weekView.setMonthChangeListener(this);
        weekView.setMinDate(Calendar.getInstance());

        credentials = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        getResultsFromApi();

        Button button = (Button) findViewById(R.id.AddEvent);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ScheduleActivity.this);
                builder.setTitle("New Event");
                builder.setView(R.layout.new_event_layout);
                final Dialog d = builder.create();
                d.show();

                final Event newEvent = new Event();
                final EditText summary = (EditText) d.findViewById(R.id.Summary);
                summary.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        closePickers(d);
                    }
                });


                final EditText date = (EditText) d.findViewById(R.id.DateText);
                date.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        closePickers(d);
                        DatePicker pickdate = (DatePicker) d.findViewById(R.id.datePicker);
                        pickdate.setVisibility(View.VISIBLE);
                        date.setVisibility(View.GONE);
                    }
                });

                final EditText startTime = (EditText) d.findViewById(R.id.TimeText);
                startTime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        closePickers(d);
                        TimePicker picktime = (TimePicker) d.findViewById((R.id.timePicker));
                        picktime.setVisibility(View.VISIBLE);
                        startTime.setVisibility(View.GONE);
                    }
                });

                final EditText endTime = (EditText) d.findViewById(R.id.timeEndText);
                endTime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        closePickers(d);
                        TimePicker endpickTime = (TimePicker) d.findViewById((R.id.timePicker2));
                        endpickTime.setVisibility(View.VISIBLE);
                        endTime.setVisibility(View.GONE);
                    }
                });


                d.findViewById(R.id.create).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        closePickers(d);
                        TimePicker picker = d.findViewById(R.id.timePicker);
                        int hour = picker.getCurrentHour();
                        int minute = picker.getCurrentMinute();
                        DatePicker datepick = d.findViewById(R.id.datePicker);
                        int day = datepick.getDayOfMonth();
                        int month = datepick.getMonth();
                        int year = datepick.getYear();

                        Calendar c = Calendar.getInstance();
                        c.set(year, month, day, hour, minute);
                        DateTime startDateTime = new DateTime(c.getTime());

                        TimePicker endPick = d.findViewById(R.id.timePicker2);
                        int endHour = endPick.getCurrentHour();
                        int endMin = endPick.getCurrentMinute();

                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month, day, endHour, endMin);
                        DateTime endDateTime = new DateTime(cal.getTime());

                        Event newEvent = new Event()
                                .setSummary(((EditText)d.findViewById(R.id.Summary)).getText().toString())
                                .setStart(new EventDateTime().setDateTime(startDateTime))
                                .setEnd(new EventDateTime().setDateTime(endDateTime));
                        HttpTransport transport = AndroidHttp.newCompatibleTransport();
                        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                        com.google.api.services.calendar.Calendar service = new com.google.api.services.calendar.Calendar.Builder(
                                transport, jsonFactory, credentials)
                                .setApplicationName(APP_NAME)
                                .build();
                        new InsertEventTask(credentials, newEvent, ScheduleActivity.this).execute();
                        System.out.println("Started InsertEventTask");
                        d.dismiss();
                    }
                });
            }
        });
    }

    private void closePickers(Dialog d) {
        d.findViewById(R.id.timePicker).setVisibility(View.GONE);
        d.findViewById(R.id.timePicker2).setVisibility(View.GONE);
        d.findViewById(R.id.DateText).setVisibility(View.VISIBLE);
        d.findViewById(R.id.TimeText).setVisibility(View.VISIBLE);
        d.findViewById(R.id.timeEndText).setVisibility(View.VISIBLE);

        DatePicker dp = (DatePicker) d.findViewById(R.id.datePicker);
        dp.setVisibility(View.GONE);
        ((EditText) d.findViewById(R.id.DateText)).setText(String.format("%d-%d-%d", dp.getMonth(), dp.getDayOfMonth(), dp.getYear()));
    }

    @Override
    public void onEventClick(WeekViewEvent event, RectF eventRect) {

    }

    @Override
    public List<? extends WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        ArrayList<WeekViewEvent> w = new ArrayList<>();
        long i = 0;
        if(week == null)
            return new ArrayList<>();
        for(Event event : week) {
            Calendar start, end;
            start = Calendar.getInstance();
            end = Calendar.getInstance();

            if (event.getStart().getDateTime() == null) {
                start.setTimeInMillis(event.getStart().getDate().getValue());
            }
            else {
                start.setTimeInMillis(event.getStart().getDateTime().getValue());
            }

            if (event.getEnd().getDate() == null) {
                end.setTimeInMillis(event.getEnd().getDateTime().getValue());
            } else {
                end.setTimeInMillis(event.getEnd().getDate().getValue());
            }

            w.add(new WeekViewEvent(i++, event.getSummary(), start, end));

        }
        return w;
    }

    @Override
    public void onEventLongPress(WeekViewEvent event, RectF eventRect) {

    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            System.out.println("GPS");
            acquireGooglePlayServices();
        } else if (credentials.getSelectedAccountName() == null) {
            System.out.println("Choose account");
            chooseAccount();
        } else if (! isDeviceOnline()) {
            System.out.println("No network connection available.");
        } else {
            System.out.println("Async task");
            new MakeRequestTask(credentials).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
//            String accountName = getPreferences(Context.MODE_PRIVATE)
//                    .getString(PREF_ACCOUNT_NAME, null);
            String accountName = null;
            if (accountName != null) {
                credentials.setSelectedAccountName(accountName);
                System.out.println(credentials.getSelectedAccountName());
                getResultsFromApi();
            } else {
                System.out.println("Null account");
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        credentials.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            System.out.println("Get permissions");
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("ActivityResult: " + requestCode + " - " + resultCode);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    System.out.println(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        credentials.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                ScheduleActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private static class InsertEventTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        private Event event;
        private ScheduleActivity mainActivity;

        InsertEventTask(GoogleAccountCredential credential, Event event, ScheduleActivity mainActivity) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Schedule")
                    .build();
            this.event = event;
            this.mainActivity = mainActivity;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            try {
                mService.events().insert("primary", event).execute();
                System.out.println("Inserted event");
                mainActivity.getResultsFromApi();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(APP_NAME)
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            List<String> eventStrings = new ArrayList<>();
            Events events = mService.events().list("primary")
                    .setTimeMin(new DateTime(weekView.getFirstVisibleDay().getTime()))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();

            week = new ArrayList<>(items);
            cycleWeekViewBecauseItSucks();

            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    // All-day events don't have start times, so just use
                    // the start date.
                    start = event.getStart().getDate();
                }
                eventStrings.add(
                        String.format("%s (%s)", event.getSummary(), start));
            }
            return eventStrings;
        }


        @Override
        protected void onPreExecute() {
//            mOutputText.setText("");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
//            mProgress.hide();
            if (output == null || output.size() == 0) {
                System.out.println("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Calendar API:");
                System.out.println(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
//            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            ScheduleActivity.REQUEST_AUTHORIZATION);
                } else {
                    System.out.println("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                System.out.println("Request cancelled.");
            }
        }
    }

    private void cycleWeekViewBecauseItSucks() {
        Calendar calendar = weekView.getFirstVisibleDay();
        calendar.add(Calendar.YEAR, 1);
        weekView.goToDate(calendar);
        calendar.add(Calendar.YEAR, -1);
        weekView.goToDate(calendar);
    }
}
