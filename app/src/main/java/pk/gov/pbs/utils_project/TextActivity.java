package pk.gov.pbs.utils_project;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pk.gov.pbs.utils.TextUtils;
import pk.gov.pbs.utils.ThemeUtils;

public class TextActivity extends AppCompatActivity {

    private static final String[] list = new String[]{
            "Item 1",
            "Item 2",
            "Item 3",
            "Item 4",
            "Item 5" 
    };
    int lsi = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_text);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ((TextView)findViewById(R.id.tvList)).setText(
                String.join(", ", list)
        );
    }

    public void applyFont(View view) {
        TextView poem = findViewById(R.id.poem);
        ThemeUtils.applyUrduFontTypeFace(poem);
    }

    public void switchList(View view) {
        TextView l = findViewById(R.id.tvList);
        if (++lsi % 2 == 0)
            l.setText(TextUtils.makeUnorderedList(list));
        else
            l.setText(String.join(", ", list));
    }
}