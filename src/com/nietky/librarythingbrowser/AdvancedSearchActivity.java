package com.nietky.librarythingbrowser;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;

public class AdvancedSearchActivity extends Activity {
    public static final String TAG = "AdvancedSearchActivity";
    SharedPreferences sharedPref;
    LogHandler logger;
    
    String allIds = "";
    String subsetIds = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String METHOD = ".onCreate()";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_search);
        
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        logger = new LogHandler(sharedPref);
        logger.log(TAG + METHOD, "Start");

        Intent intent = getIntent();

        CheckBox checkBox = (CheckBox) findViewById(R.id.advanced_search_restrict_ids_checkbox);
        final String whereFrom = intent.getStringExtra("whereFrom");
        logger.log(TAG, "whereFrom = " + whereFrom);
        if (checkBox.isChecked())
            checkBox.setChecked(true);
        else
            checkBox.setChecked(false);
        
        if (whereFrom.contains("menu"))
            checkBox.setText("Restrict to books shown on previous screen");
        else if (whereFrom.contains("searchQuery"))
            checkBox.setText(Html.fromHtml("Refine search <i>results</i> rather than the original search"));
        
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (whereFrom.contains("menu")) {
                        buttonView.setText("Within books shown on previous screen");
                    } else
                        buttonView.setText("In all books");
                } else {
                    if (whereFrom.contains("searchQuery"))
                        buttonView.setText("Within search <i>results</i>");
                    else
                        buttonView.setText("Modify original search");
                }
            }
        });



        SearchHandler searchHandler = new SearchHandler(this);
        searchHandler.setIds();
        String booksAllIds = searchHandler.getString();

        if (intent.hasExtra("query")) {
            EditText queryField = (EditText) findViewById(R.id.advanced_search_query_text);
            queryField.setText(intent.getStringExtra("query"));
        }
        if (intent.hasExtra("allIds"))
            allIds = intent.getStringExtra("allIds");
        else
            allIds = booksAllIds;
        if (intent.hasExtra("subsetIds"))
            subsetIds = intent.getStringExtra("subsetIds");
        else
            subsetIds = allIds;
        if (subsetIds == allIds) {
            checkBox.setVisibility(View.INVISIBLE);
        }

        Button selectFields = (Button) findViewById(R.id.advanced_search_select_fields_button);
        selectFields.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SelectItemsActivity.class);
                intent.putExtra("type", SelectItemsActivity.TYPE_FIELDS);
                intent.putExtra("ids", getSearchIds());
                startActivityForResult(intent, 0);
            }
        });

        Button restrictCollections = (Button) findViewById(R.id.advanced_search_collections_button);
        restrictCollections.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SelectItemsActivity.class);
                intent.putExtra("type", SelectItemsActivity.TYPE_COLLECTIONS);
                intent.putExtra("ids", getSearchIds());
                startActivityForResult(intent, 0);
            }
        });

        Button restrictTags = (Button) findViewById(R.id.advanced_search_tags_button);
        restrictTags.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SelectItemsActivity.class);
                intent.putExtra("type", SelectItemsActivity.TYPE_TAGS);
                intent.putExtra("ids", getSearchIds());
                startActivityForResult(intent, 0);
            }
        });
    }

    public String getSearchIds() {
        CheckBox checkBox = (CheckBox) findViewById(R.id.advanced_search_restrict_ids_checkbox);
        String ids = "";
        if (checkBox.isChecked())
            ids = subsetIds;
        else
            ids = allIds;
        return ids;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.advanced_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static public class SelectItemsDialogFragment extends DialogFragment {
        public static final String TAG = "SelectItemsDialogFragment";

        public static final int TYPE_FIELDS = 0;
        public static final int TYPE_COLLECTIONS = 1;
        public static final int TYPE_TAGS = 2;

        private int type;
        private SearchHandler searchHandler;
        private String ids = "";

        @Override
        public void setArguments(Bundle bundle) {
            this.type = bundle.getInt("type");
            this.ids = bundle.getString("ids");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View parentView = inflater.inflate(
                    R.layout.dialog_select_something, null);
            builder.setView(parentView);
            ListView listView = (ListView) parentView
                    .findViewById(R.id.dialog_list);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_list_item_multiple_choice);
            ArrayList<String> array = getArrayOfItems();
            for (int i = 0; i < array.size(); i++) {
                adapter.add(array.get(i));
            }
            listView.setAdapter(adapter);
            listView.setFastScrollEnabled(true);
            builder.setTitle(getTitle());
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            builder.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            return builder.create();
        }

        public String getTitle() {
            String string = "";
            switch (type) {
            case TYPE_FIELDS:
                string = "Select fields to search";
                break;
            case TYPE_COLLECTIONS:
                string = "Restrict to collections";
                break;
            case TYPE_TAGS:
                string = "Restrict to tags";
                break;
            }
            return string;
        }

        public ArrayList<String> getArrayOfItems() {
            ArrayList<String> array = new ArrayList<String>();
            if (type == TYPE_FIELDS) {
                array.add("Comments");
                array.add("Publication details");
                array.add("Title");
            } else {
                CursorTags cursorTags = new CursorTags();
                SearchHandler searchHandler = new SearchHandler(getActivity());
                searchHandler.setIds(ids);
                Cursor cursor = searchHandler.getCursor();
                switch (type) {
                case TYPE_COLLECTIONS:
                    array = cursorTags.getCollections(cursor);
                    Collections.sort(array);
                    break;
                case TYPE_TAGS:
                    array = cursorTags.getTags(cursor);
                    Collections.sort(array);
                    break;
                }
                searchHandler.close();
            }
            return array;
        }
    }
}