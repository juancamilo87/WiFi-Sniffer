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
                if(!serviceStarted){
                    startService(startService);
                    serviceStarted = !serviceStarted;
                    btn_toggle.setText("Stop service");
                }
                else {
                    stopService(startService);
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
                    txt_file.setText("");
                    try {
                        FileReader fr=new FileReader(selectedfile);
                        BufferedReader br=new BufferedReader(fr);
                        String line = null;
                        try {
                            while((line = br.readLine()) != null)
                            {
                                txt_file.append(line);
                                txt_file.append("\n");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                handler.postDelayed(readFile,10*1000);
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
                    Log.d("FILE",selectedfile.getName());
                }
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
}
