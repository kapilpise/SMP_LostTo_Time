package bluetoothchat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.android.bluetoothchat.R;

import java.util.ArrayList;

import common.model.Utility;


public class SettingActivity extends AppCompatActivity {
Spinner spnTime;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        spnTime =(Spinner)findViewById(R.id.spnTime);
        final ArrayList<String> strings= new ArrayList();
        strings.add("5 sec");
        strings.add("10 sec");
        strings.add("15 sec");
        strings.add("20 sec");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.select_dialog_singlechoice,android.R.id.text1,strings);
        spnTime.setAdapter(adapter);
        spnTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Utility.selectedTimeDuration=strings.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
}
