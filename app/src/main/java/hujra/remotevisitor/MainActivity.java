package hujra.remotevisitor;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button connect_button = (Button) findViewById(R.id.connectButton);
        final EditText IPtext = (EditText) findViewById(R.id.editIP);
        final Context me = this;
        connect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(me, VRActivity2.class);
                intent.putExtra("IPtext", IPtext.getText().toString());
                startActivity(intent);
            }
        });
    }
}