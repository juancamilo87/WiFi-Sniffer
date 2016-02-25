package fi.oulu.wifimacaddresssniffer;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileParser extends Service {
    public FileParser() {
    }

    private Handler readFileHandler;
    private Handler createJSONHandler;

    private Runnable readFile;
    private Runnable createJSON;

    private static final String PATH = "/ACP_JSON/mac_addresses.json";

    @Override
    public void onCreate() {
        super.onCreate();

        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.sym_def_app_icon)
                        .setContentTitle("Wifi Sniffer")
                        .setContentText("File Parser Running");

        startForeground(1336, mBuilder.build());

        readFileHandler = new Handler();
        createJSONHandler = new Handler();

        readFile = new Runnable() {
            private File selectedfile;

            @Override
            public void run() {
                chooseFile();
                if(selectedfile != null)
                {
                    List<String> allLines = new ArrayList<String>();

                    try {
                        FileReader fr=new FileReader(selectedfile);
                        BufferedReader br=new BufferedReader(fr);
                        String line = null;
                        try {
                            while((line = br.readLine()) != null)
                            {
                                allLines.add(line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    if(allLines.size()>0)
                    {
                        boolean shouldStore = false;
                        for(int i = 1; i< allLines.size(); i++)
                        {
                            if(allLines.get(i-1).contains("Station MAC"))
                            {
                                shouldStore = true;
                            }
                            if(shouldStore)
                            {
                                String[] values = allLines.get(i).split(",");
                                if(values!=null && values.length > 3)
                                {
                                    long result = DatabaseHelper.insert(getApplicationContext(),values[0],Integer.valueOf(values[3].trim()),values[2]);
                                    if(result==-2)
                                    {
                                        Log.d(MainActivity.TAG,"Mac address updated");
                                    }
                                    else if(result==-1)
                                    {
                                        Log.d(MainActivity.TAG,"Error inserting mac address");
                                    }
                                    else
                                    {
                                        Log.d(MainActivity.TAG, "New Mac address found");
                                    }
                                }
                            }
                        }
                    }
                }
                readFileHandler.postDelayed(readFile, 5 * 1000);
            }

            private void chooseFile(){
                String path = Environment.getExternalStorageDirectory().toString()+"/ACP_files/";
                File f = new File(path);
                File[] files = f.listFiles();
                long lastModified = 0;
                File toRead = null;
                for (File inFile : files) {
                    if (!inFile.isDirectory()) {
                        if(inFile.lastModified()>lastModified) {
                            lastModified = inFile.lastModified();
                            toRead = inFile;
                        }
                    }
                }
                if(toRead!=null) {
                    selectedfile = toRead;
                    Log.d(MainActivity.TAG, "Read: " + selectedfile.getName());
                }
            }
        };

        createJSON = new Runnable() {
            @Override
            public void run() {
                Cursor cursor = DatabaseHelper.read(getApplicationContext(),System.currentTimeMillis()/1000-120);
                JSONArray macsJSON = new JSONArray();
                if(cursor != null)
                {
                    if (cursor.moveToFirst()){
                        do{
                            JSONObject thisMac = new JSONObject();
                            String mac_address = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MacAddress.MAC_ADDRESS));
                            int signal_strength = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.MacAddress.SIGNAL_STRENGTH));
                            long last_seen = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.MacAddress.LAST_SEEN_EPOCH));

                            try {
                                thisMac.put("mac_address",mac_address);
                                thisMac.put("signal_strength",signal_strength);
                                thisMac.put("last_seen",last_seen);

                                macsJSON.put(thisMac);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }while(cursor.moveToNext());
                    }
                    cursor.close();
                }

                JSONObject finalJSON = new JSONObject();
                try {
                    finalJSON.put("token","1234567890");
                    finalJSON.put("devices",macsJSON);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String content = finalJSON.toString();
                FileWriter fw = null;
                try {
                    fw = new FileWriter(Environment.getExternalStorageDirectory().toString() + PATH);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write(content);
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                createJSONHandler.postDelayed(createJSON,10*1000);
            }
        };

        readFileHandler.post(readFile);
        createJSONHandler.post(createJSON);
    }






    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        readFileHandler.removeCallbacks(readFile);
        createJSONHandler.removeCallbacks(createJSON);
        super.onDestroy();
    }
}
