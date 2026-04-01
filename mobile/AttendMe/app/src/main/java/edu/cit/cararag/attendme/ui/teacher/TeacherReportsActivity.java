package edu.cit.cararag.attendme.ui.teacher;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import edu.cit.cararag.attendme.R;

public class TeacherReportsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: Make sure activity_teacher_reports exists in your layout folder
        setContentView(R.layout.activity_teacher_reports);

        // Quick Mock Data implementation using a ListView
        ListView listView = findViewById(R.id.reportsListView);

        ArrayList<String> mockReports = new ArrayList<>();
        mockReports.add("Student: Trisha - Status: Present - April 01");
        mockReports.add("Student: John - Status: Absent - April 01");
        mockReports.add("Student: Maria - Status: Late - April 01");
        mockReports.add("Student: Alex - Status: Present - April 01");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                mockReports
        );

        if (listView != null) {
            listView.setAdapter(adapter);
        }
    }
}