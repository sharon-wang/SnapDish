package htn2017team.snapdish;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by Elliot on 9/17/2017.
 */

public class RecipeSuggestion extends AppCompatActivity {
    Button button;
    LinearLayout layout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipesuggestion);

        button = (Button) findViewById(R.id.submit);
        layout = (LinearLayout) findViewById(R.id.success);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setVisibility(View.VISIBLE);
            }
        });
    }
}
