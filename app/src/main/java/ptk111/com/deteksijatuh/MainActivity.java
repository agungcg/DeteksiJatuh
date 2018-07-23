package ptk111.com.deteksijatuh;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PICK_CONTACT = 1;
    private boolean isSensorOn;
    private String stringNumber, stringName, stringMsg = "Inna lillahi wa inna ilaihi raji'un saudara anda telah terjatuh, mohon diberikan pertolangan secepatnya.";
    private long pattern[]={0,300,200,300,500};

    private SensorManager sensorManager;
    private Sensor gyroscope;

    //----------------------------------------------------------------------------------------------

    int statusALow = 0;
    int statusTLow = 0;

    int i = 0;

    double TH1 = 4.329318653;
    double TH2 = 0.213216589;
    double TH3 = 0.417569941;

    boolean statusTH1 = false;
    boolean statusTH2 = false;
    boolean statusTH3 = false;
    boolean statusFall = false;
    boolean statusNotif = false;

    //----------------------------------------------------------------------------------------------

    long startWt = 0L, wtInMiliseconds = 0L, wtSwapBuff = 0L, updateWt = 0L;
    double tempWx, tempWy, tempWz, tempWres;

    ArrayList<Integer> iMax = new ArrayList<Integer>();
    ArrayList<Long> wtMax = new ArrayList<Long>();
    ArrayList<Double> wresMax = new ArrayList<Double>();

    ArrayList<Long> wtPublic = new ArrayList<Long>();
    ArrayList<Double> wxPublic = new ArrayList<Double>();
    ArrayList<Double> wyPublic = new ArrayList<Double>();
    ArrayList<Double> wzPublic = new ArrayList<Double>();
    ArrayList<Double> wresPublic = new ArrayList<Double>();

    //----------------------------------------------------------------------------------------------

    double tempAtLow;
    double tempAxLow, tempAyLow, tempAzLow;

    double tempAtHigh;
    double tempAxHigh, tempAyHigh, tempAzHigh;

    //----------------------------------------------------------------------------------------------

    double tempTtLow;
    double tempTxLow, tempTyLow, tempTzLow;

    double tempTtHigh;
    double tempTxHigh, tempTyHigh, tempTzHigh;

    //----------------------------------------------------------------------------------------------

    TextView tvNama, tvNoTelepon;
    ImageButton btnSwitch;
    Button btnContact;
    Vibrator vibe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSwitch = (ImageButton) findViewById(R.id.btnSwitch);
        btnContact = (Button) findViewById(R.id.btnContact);

        tvNama = (TextView) findViewById(R.id.tvNama);
        tvNoTelepon = (TextView) findViewById(R.id.tvNoTelepon);

        vibe = (Vibrator) getSystemService(MainActivity.VIBRATOR_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        toggleButtonImage();

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSensorOn) {
                    turnOffSensor();
                } else {
                    turnOnSensor();
                }
            }
        });

        btnContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT);
            }
        });

    }

    private void turnOnSensor() {
        if (!isSensorOn) {
            i = 0;

            isSensorOn = true;

            statusTH1 = false;
            statusTH2 = false;
            statusTH3 = false;
            statusFall = false;
            statusNotif = false;

            startWt = SystemClock.uptimeMillis();

            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if(gyroscope != null) {
                sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
            }else {
                Toast.makeText(MainActivity.this, "Perangkat Tidak Mendukung Gyroscope", Toast.LENGTH_LONG).show();
            }

            vibe.vibrate(500);

            toggleButtonImage();
        }
    }

    private void turnOffSensor() {
        if (isSensorOn) {
            isSensorOn = false;

            sensorManager.unregisterListener(MainActivity.this);

            wtPublic.clear();
            wxPublic.clear();
            wyPublic.clear();
            wzPublic.clear();
            wresPublic.clear();
            iMax.clear();
            wtMax.clear();
            wresMax.clear();

            startWt = 0L;
            wtInMiliseconds = 0L;
            wtSwapBuff = 0L;
            updateWt = 0L;

            toggleButtonImage();
        }
    }

    private void toggleButtonImage(){
        if(isSensorOn){
            btnSwitch.setImageResource(R.drawable.btn_switch_on);
        }else {
            btnSwitch.setImageResource(R.drawable.btn_switch_off);
        }
    }


    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if(reqCode == PICK_CONTACT){

                Uri returnUri = data.getData();
                Cursor cursor = getContentResolver().query(returnUri, null, null, null, null);

                if(cursor.moveToNext()) {
                    int columnIndex_ID = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                    String contactID = cursor.getString(columnIndex_ID);

                    int columnIndex_HASPHONENUMBER = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    String stringHasPhoneNumber = cursor.getString(columnIndex_HASPHONENUMBER);
                    if (stringHasPhoneNumber.equalsIgnoreCase("1")) {
                        Cursor cursorNum = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID,
                                null,
                                null);
                        if (cursorNum.moveToNext()) {
                            int columnIndex_number = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            int columnIndex_name = cursorNum.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);

                            stringNumber = cursorNum.getString(columnIndex_number);
                            stringName = cursorNum.getString(columnIndex_name);

                            tvNama.setText(stringName);
                            tvNoTelepon.setText(stringNumber);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Data Nomer Kontak Tidak Ada!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        long dtALow, dtTLow;
        int nALow, nTLow;

        Sensor sensor = sensorEvent.sensor;

        if(sensor.getType() == Sensor.TYPE_GYROSCOPE){

            wtInMiliseconds = (SystemClock.uptimeMillis()) - startWt;
            updateWt = wtSwapBuff + wtInMiliseconds;

            tempWx = sensorEvent.values[0];
            tempWy = sensorEvent.values[1];
            tempWz = sensorEvent.values[2];
            tempWres = Math.sqrt((tempWx * tempWx) + (tempWy * tempWy) + (tempWz * tempWz));

            wtPublic.add(updateWt);
            wxPublic.add(tempWx);
            wyPublic.add(tempWy);
            wzPublic.add(tempWz);
            wresPublic.add(tempWres);

            if(tempWres > TH1){
                statusALow = 0;
                statusTLow = 0;
                statusTH1 = true;

                iMax.add(wtPublic.indexOf(updateWt));
                wtMax.add(updateWt);
                wresMax.add(tempWres);
            }

            if(i < wresMax.size()) {
                int m = 1, n = 1;
                int tempIMax = iMax.get(i);

                while (statusALow == 0) {
                    if (wtPublic.indexOf(wtPublic.get(tempIMax - m)) >= 0) {
                        dtALow = Math.abs(wtMax.get(i) - wtPublic.get(tempIMax - m));
                        if (dtALow > 500) {
                            statusALow = 1;

                            nALow = tempIMax - m;

                            tempAtLow = (double) wtPublic.get(nALow) / 1000;
                            tempAxLow = wxPublic.get(nALow);
                            tempAyLow = wyPublic.get(nALow);
                            tempAzLow = wzPublic.get(nALow);
                        }
                    } else {

                    }
                    m++;
                }

                while (statusTLow == 0) {
                    if (wtPublic.indexOf(wtPublic.get(tempIMax - n)) >= 0) {
                        dtTLow = Math.abs(wtMax.get(i) - wtPublic.get(tempIMax - n));
                        if (dtTLow > 1200) {
                            statusTLow = 1;

                            nTLow = tempIMax - n;

                            tempTtLow = (double) wtPublic.get(nTLow) / 1000;
                            tempTxLow = wxPublic.get(nTLow);
                            tempTyLow = wyPublic.get(nTLow);
                            tempTzLow = wzPublic.get(nTLow);
                        }
                    } else {

                    }
                    n++;
                }
                i++;

                getHigh(tempAtLow, tempAxLow, tempAyLow, tempAzLow, tempTtLow, tempTxLow, tempTyLow, tempTzLow);

            }

            if (statusNotif){

                sensorManager.unregisterListener(MainActivity.this);

                startWt = 0L;
                wtInMiliseconds = 0L;
                wtSwapBuff = 0L;
                updateWt = 0L;

                wtPublic.clear();
                wxPublic.clear();
                wyPublic.clear();
                wzPublic.clear();
                wresPublic.clear();
                iMax.clear();
                wtMax.clear();
                wresMax.clear();

                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Jatuh Terdeteksi")
                        .setMessage("Apakah Anda Baik-Baik Saja?")
                        .setPositiveButton(Html.fromHtml("<font>Ya</font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                isSensorOn = false;
                                vibe.cancel();
                                turnOnSensor();
                            }
                        })
                        .setNegativeButton(Html.fromHtml("<font>Tidak</font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //SmsManager.getDefault().sendTextMessage(stringNumber, null, stringMsg, null,null);
                                Toast.makeText(MainActivity.this, "Pesan Terkirim", Toast.LENGTH_LONG).show();
                                vibe.cancel();
                                turnOffSensor();
                            }
                        })
                        .setCancelable(false)
                        .create();

                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    private static final int AUTO_DISMISS_MILLIS = 6000;
                    @Override
                    public void onShow(final DialogInterface dialog) {
                        final Button defaultButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                        final CharSequence positiveButtonText = defaultButton.getText();
                        new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                defaultButton.setText(String.format(Locale.getDefault(), "%s (%d)",positiveButtonText, TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                                ));

                            }
                            @Override
                            public void onFinish() {
                                if (((AlertDialog) dialog).isShowing()) {
                                    //SmsManager.getDefault().sendTextMessage(stringNumber, null, stringMsg, null,null);
                                    Toast.makeText(MainActivity.this, "Pesan Terkirim", Toast.LENGTH_LONG).show();
                                    vibe.cancel();
                                    turnOffSensor();
                                    dialog.dismiss();
                                }
                            }
                        }.start();
                    }
                });
                dialog.show();
                vibe.vibrate(pattern,0);
            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void getHigh(final double tempAtLow, final double tempAxLow, final double tempAyLow, final double tempAzLow, final double tempTtLow, final double tempTxLow, final double
            tempTyLow, final double tempTzLow){
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */

                tempAtHigh = (double)updateWt/1000;
                tempAxHigh = tempWx;
                tempAyHigh = tempWy;
                tempAzHigh = tempWz;

                double tempAx = (tempAxHigh - tempAxLow) / (tempAtHigh - tempAtLow);
                double tempAy = (tempAyHigh - tempAyLow) / (tempAtHigh - tempAtLow);
                double tempAz = (tempAzHigh - tempAzLow) / (tempAtHigh - tempAtLow);
                double tempAres = Math.sqrt((tempAx * tempAx) + (tempAy * tempAy) + (tempAz * tempAz));

                //----------------------------------------------------------------------------------

                tempTtHigh = (double)updateWt/1000;
                tempTxHigh = tempWx;
                tempTyHigh = tempWy;
                tempTzHigh = tempWz;

                double tempTx = ((tempTxHigh - tempTxLow) * (tempTtHigh - tempTtLow));
                double tempTy = ((tempTyHigh - tempTyLow) * (tempTtHigh - tempTtLow));
                double tempTz = ((tempTzHigh - tempTzLow) * (tempTtHigh - tempTtLow));
                double tempTres = Math.sqrt((tempTx * tempTx) + (tempTy * tempTy) + (tempTz * tempTz));

                if (tempAres > TH2){
                    statusTH2 = true;
                }else {
                    statusTH2 = false;
                }

                if (tempTres > TH3){
                    statusTH3 = true;
                }else {
                    statusTH3 = false;
                }

                if (statusTH2 && statusTH3){
                    statusFall = true;
                    statusNotif = true;
                }else{
                    statusFall = false;
                }

            }
        }, 500);
    }
}
