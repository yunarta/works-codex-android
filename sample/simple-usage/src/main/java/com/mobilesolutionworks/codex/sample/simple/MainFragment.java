package com.mobilesolutionworks.codex.sample.simple;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobilesolutionworks.codex.ActionHook;
import com.mobilesolutionworks.codex.PropertySubscriber;

/**
 * Created by yunarta on 6/11/15.
 */
public class MainFragment extends Fragment
{
    class FragmentInternalHook
    {
        @PropertySubscriber(":activity:property")
        public void onPropertyChanged(String property)
        {
            mProperty = property;

            if (isVisible()) postUpdate();
        }

        @ActionHook(":activity:actionA")
        public void actionHookA(double value)
        {
            if (isVisible())
            {
                TextView textView = (TextView) getView().findViewById(R.id.text_action);
                textView.setText("actionA " + value);
            }
        }

        @ActionHook(":activity:actionB")
        public void actionHookB(String value)
        {
            if (isVisible())
            {
                TextView textView = (TextView) getView().findViewById(R.id.text_action);
                textView.setText("actionB " + value);
            }
        }
    }

    FragmentInternalHook mHook;

    String mProperty;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mHook = new FragmentInternalHook();
        CodexSingleton.SINGLETON.register(mHook);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        postUpdate();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        CodexSingleton.SINGLETON.unregister(mHook);
    }

    @SuppressWarnings("ConstantConditions")
    private void postUpdate()
    {
        TextView textView = (TextView) getView().findViewById(R.id.text_property);
        textView.setText(mProperty);
    }
}
