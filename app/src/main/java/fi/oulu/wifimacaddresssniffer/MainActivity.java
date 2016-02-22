package fi.oulu.wifimacaddresssniffer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS = "fi.oulu.wifimacaddresssniffer";
    public static final String SERVICE = "serviceRunning";

    private Button btn_toggle;
    private TextView txt_file;

    private boolean serviceStarted;
    private Handler handler;
    private Runnable readFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_toggle = (Button) findViewById(R.id.btn_toggle);
        txt_file = (TextView) findViewById(R.id.txt_file);
        SharedPreferences preferences = getSharedPreferences(PREFS,MODE_PRIVATE);
        serviceStarted = preferences.getBoolean(SERVICE,false);
        if(serviceStarted) {
            btn_toggle.setText("Stop service");
        }
        else {
            btn_toggle.setText("Start service");
        }

        btn_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startService = new Intent(getApplicationContext(),MacSniffer.class);
                Intent startOtherService = new Intent(getApplicationContext(),FileParser.class);
                if(!serviceStarted){
                    startService(startService);
                    startService(startOtherService);
                    serviceStarted = !serviceStarted;
                    btn_toggle.setText("Stop service");
                }
                else {
                    stopService(startService);
                    stopService(startOtherService);
                    serviceStarted = !serviceStarted;
                    btn_toggle.setText("Start service");
                }
            }
        });

        handler = new Handler();

        readFile = new Runnable() {
            private File selectedfile;
            @Override
            public void run() {
                chooseFile();
                if(selectedfile != null)
                {
                    StringBuilder builder = new StringBuilder();
                    txt_file.setText("");
                    try {
                        FileReader fr=new FileReader(selectedfile);
                        BufferedReader br=new BufferedReader(fr);
                        String line = null;
                        try {
                            while((line = br.readLine()) != null)
                            {
                                builder.append(line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    String jsonString = builder.toString();

                    txt_file.setText(formatString(jsonString));

                }
                handler.postDelayed(readFile,10*1000);
            }

            private void chooseFile(){
                String path = Environment.getExternalStorageDirectory().toString()+"/ACP_JSON/mac_addresses.json";
                File f = new File(path);
                selectedfile = f;
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(readFile);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(readFile);
    }

    public static String formatString(String text){

        StringBuilder json = new StringBuilder();
        String indentString = "";

        for (int i = 0; i < text.length(); i++) {
            char letter = text.charAt(i);
            switch (letter) {
                case '{':
                case '[':
                    json.append("\n" + indentString + letter + "\n");
                    indentString = indentString + "\t";
                    json.append(indentString);
                    break;
                case '}':
                case ']':
                    indentString = indentString.replaceFirst("\t", "");
                    json.append("\n" + indentString + letter);
                    break;
                case ',':
                    json.append(letter + "\n" + indentString);
                    break;

                default:
                    json.append(letter);
                    break;
            }
        }

        return json.toString();
    }
}
