package com.mobilesolutionworks.codex.sample.simple;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import com.mobilesolutionworks.codex.Property;


/**
 * Created by yunarta on 6/11/15.
 */
public class MainActivity extends AppCompatActivity
{
    class InternalHook
    {
        String mActivityProperty;

        public void updateProperty(String property)
        {
            mActivityProperty = property;
            CodexSingleton.SINGLETON.updateProperty(mHook, ":activity:property");

            EditText editText = (EditText) findViewById(R.id.edt_property);
            editText.setText(mActivityProperty);
        }

        @Property(":activity:property")
        public String getProperty()
        {
            return mActivityProperty;
        }
    }

    InternalHook mHook;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_add_fragment).setOnClickListener(new MyOnClickListener());
        findViewById(R.id.btn_remove_fragment).setOnClickListener(new MyOnClickListener());
        findViewById(R.id.btn_property).setOnClickListener(new MyOnClickListener());
        findViewById(R.id.btn_action_a).setOnClickListener(new MyOnClickListener());
        findViewById(R.id.btn_action_b).setOnClickListener(new MyOnClickListener());

        mHook = new InternalHook();
        CodexSingleton.SINGLETON.register(mHook);

        mHook.updateProperty("Default Property");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        CodexSingleton.SINGLETON.unregister(mHook);
    }

    private class MyOnClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.btn_add_fragment:
                {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_container, new MainFragment(), "root").commit();
                    break;
                }

                case R.id.btn_remove_fragment:
                {
                    FragmentManager fm = getFragmentManager();
                    Fragment root = fm.findFragmentByTag("root");

                    if (root != null)
                    {
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.remove(root).commit();
                    }
                    break;
                }

                case R.id.btn_property:
                {
                    EditText editText = (EditText) findViewById(R.id.edt_property);

                    Editable text = editText.getText();
                    mHook.updateProperty(String.valueOf(text));
                    break;
                }

                case R.id.btn_action_a:
                {
                    CodexSingleton.SINGLETON.startAction(":activity:actionA", Math.random());
                    break;
                }

                case R.id.btn_action_b:
                {
                    CodexSingleton.SINGLETON.startAction(":activity:actionB", String.valueOf(Math.random()));
                    break;
                }
            }
        }
    }
}